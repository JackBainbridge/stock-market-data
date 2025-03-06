package stock.market.data.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import stock.market.data.configuration.DockerDetectionConfiguration;
import stock.market.data.events.RunnerCompletedEvent;
import stock.market.data.events.SchemaGeneratedEvent;
import stock.market.data.models.ColumnInformationSchemaFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(2)
public class SchemaFileGenerator implements CommandLineRunner, ApplicationListener<RunnerCompletedEvent> {
    private final Logger logger = LogManager.getLogger(SchemaFileGenerator.class);

    @Value("${src.main.resources}")
    private String SRC_MAIN_RESOURCES;

    @Value("${data.filename}")
    private String DATA_FILE_NAME;

    @Value("${schema.filename}")
    private String SCHEMA_FILE_NAME;

    @Value("${output.docker.path}")
    private String DOCKER_OUTPUT_PATH;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DockerDetectionConfiguration dockerDetectionConfiguration;
    private final ApplicationContext context;
    private boolean firstRunnerCompleted;

    public SchemaFileGenerator(ApplicationEventPublisher applicationEventPublisher,
                               DockerDetectionConfiguration dockerDetectionConfiguration, ApplicationContext context) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.dockerDetectionConfiguration = dockerDetectionConfiguration;
        this.context = context;
    }

    @Override
    public void onApplicationEvent(RunnerCompletedEvent event) {
        if (event.getRunnerClass().equals(DataFileGenerator.class)) {
            this.firstRunnerCompleted = true;
        }
    }

    @Override
    public void run(String... args) {
        if (!firstRunnerCompleted) {
            logger.info("The first order runner has not completed. SchemaFileGenerator will not be executed.");
            return;
        }

        String dataFile;
        if (dockerDetectionConfiguration.isRunningInDocker()) {
            dataFile = String.format("%s/%s", DOCKER_OUTPUT_PATH, DATA_FILE_NAME);
        } else {
            dataFile = String.format("%s/%s", SRC_MAIN_RESOURCES, DATA_FILE_NAME);
        }

        logger.info("SchemaFileGenerator datafile is: {}", dataFile);
        Map<String, Map<String, ColumnInformationSchemaFile>> dataAnalyzed = analyzeDataFileSql(dataFile);
        if (dataAnalyzed == null || dataAnalyzed.isEmpty()) {
            logger.error("Error has occurred! Data has NOT been analyzed via method: analyzeDataSql. Investigate.");

            // Shutdown the application
            ((ConfigurableApplicationContext) context).close();
        }

        boolean schemaFileGenerated = generateSchemaFileBasedOnDataFile(dataAnalyzed);
        if (!schemaFileGenerated) {
            logger.error("Error has occurred! schema file has not been generated please investigate.");

            // Shutdown the application
            ((ConfigurableApplicationContext) context).close();
        }

        // Publish application events that will trigger the other runners.
        applicationEventPublisher.publishEvent(new SchemaGeneratedEvent(this));
        applicationEventPublisher.publishEvent(new RunnerCompletedEvent(this, SchemaFileGenerator.class));
    }

    /**
     * Analyzes the created data.sql in order to dynamically set the column length based on values in the file.
     * @param dataFile
     * @return
     */
    private Map<String, Map<String, ColumnInformationSchemaFile>> analyzeDataFileSql(String dataFile) {
        Map<String, Map<String, ColumnInformationSchemaFile>> tableColumnInfo = new HashMap<>();
        Path dataPath = Paths.get(dataFile);
        try {
            List<String> lines = Files.readAllLines(dataPath);
            for (String line : lines) {
                if (line.startsWith("INSERT INTO")) {
                    Matcher matcher = Pattern.compile("INSERT INTO (\\w+) \\((.+?)\\) VALUES \\((.+?)\\);").matcher(line);
                    if (matcher.find()) {
                        String tableName = matcher.group(1);
                        String[] columns = matcher.group(2).split(",\\s*");
                        String[] values = matcher.group(3).split(",\\s*");

                        tableColumnInfo.putIfAbsent(tableName, new HashMap<>());
                        Map<String, ColumnInformationSchemaFile> columnInfoMap = tableColumnInfo.get(tableName);

                        // Loop the column values in order to determine length of values required.
                        for (int i = 0; i < columns.length; i++) {
                            String column = columns[i].trim();
                            String value = values[i].trim().replaceAll("^'|'$", "");
                            ColumnInformationSchemaFile info = columnInfoMap.getOrDefault(column, new ColumnInformationSchemaFile());

                            info.maxLength = Math.max(info.maxLength, value.length());
                            info.isNumeric &= value.matches("-?\\d+(\\.\\d+)?");
                            if (info.isNumeric) {
                                info.maxDecimalLength = value.contains(".") ? value.split("\\.")[1].length() : 0;
                            }

                            columnInfoMap.put(column, info);
                        }
                    } else {
                        logger.error("matcher.find() has returned false! Please investigate.");
                        return null;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("IOException has occurred. Please investigate. Error: {}", e.getMessage());
            return null;
        }
        return tableColumnInfo;
    }

    /**
     * Generate the schema.sql file to create our DDL for the H2 Database based off column length of data retrieved.
     * @param tableColumnInfo
     * @return
     */
    private boolean generateSchemaFileBasedOnDataFile(Map<String, Map<String, ColumnInformationSchemaFile>> tableColumnInfo) {
        Path schemaPath;
        if (dockerDetectionConfiguration.isRunningInDocker()) {
            schemaPath = Paths.get(String.format("%s/%s", DOCKER_OUTPUT_PATH, SCHEMA_FILE_NAME));
        } else {
            schemaPath = Paths.get(String.format("%s/%s", SRC_MAIN_RESOURCES, SCHEMA_FILE_NAME));
        }

        List<String> schemaLines = new ArrayList<>();
        for (Map.Entry<String, Map<String, ColumnInformationSchemaFile>> tableEntry : tableColumnInfo.entrySet()) {
            String tableName = tableEntry.getKey();
            Map<String, ColumnInformationSchemaFile> columnInfo = tableEntry.getValue();

            // Drop the table if it possibly exists before
            schemaLines.add("DROP TABLE IF EXISTS " + tableName + ";\n");
            schemaLines.add("CREATE TABLE " + tableName + " (");
            schemaLines.add("\tid BIGINT AUTO_INCREMENT PRIMARY KEY,");
            List<String> columnDefinitions = new ArrayList<>();

            for (Map.Entry<String, ColumnInformationSchemaFile> columnEntry : columnInfo.entrySet()) {
                String column = columnEntry.getKey();
                ColumnInformationSchemaFile info = columnEntry.getValue();

                String dataType = info.isNumeric ? "DECIMAL(" + (info.maxLength) + "," + (info.maxDecimalLength) + ")"
                        : "VARCHAR(" + (info.maxLength) + ")";

                columnDefinitions.add("\t" + column + " " + dataType);
            }

            schemaLines.add(String.join(",\n", columnDefinitions));
            schemaLines.add(");");
            schemaLines.add("");
        }

        try {
            Files.write(schemaPath, schemaLines);
        } catch (IOException e) {
            logger.error("IOException has occurred. Please investigate. Error: {}", e.getMessage());
            return false;
        }

        return true;
    }
}
