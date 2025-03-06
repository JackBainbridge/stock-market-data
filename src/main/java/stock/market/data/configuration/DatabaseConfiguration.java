package stock.market.data.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import stock.market.data.events.SchemaGeneratedEvent;
import stock.market.data.utility.FileUtil;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class DatabaseConfiguration implements ApplicationListener<SchemaGeneratedEvent> {
    private final Logger logger = LogManager.getLogger(DatabaseConfiguration.class);

    @Value("${src.main.resources}")
    private String SRC_MAIN_RESOURCES;

    @Value("${schema.filename}")
    private String SCHEMA_FILE_NAME;

    @Value("${output.docker.path}")
    private String DOCKER_OUTPUT_PATH;

    private final DataSource dataSource;
    private final DockerDetectionConfiguration dockerDetectionConfiguration;
    private final FileUtil fileUtil;

    public DatabaseConfiguration(DataSource dataSource,
                                 DockerDetectionConfiguration dockerDetectionConfiguration,
                                 FileUtil fileUtil) {
        this.dataSource = dataSource;
        this.dockerDetectionConfiguration = dockerDetectionConfiguration;
        this.fileUtil = fileUtil;
    }

    /**
     * Application event detection, only runs on a SchemaGeneratedEvent.
     * @param event
     */
    @Override
    public void onApplicationEvent(SchemaGeneratedEvent event) {
        String schemaFile;
        if (dockerDetectionConfiguration.isRunningInDocker()) {
            logger.info("We are currently executing inside of a docker container - Output file path will be modified.");
            schemaFile = validateDockerOutputFileLocation();
            if (schemaFile == null) {
                logger.error("Unable to validate Docker Output File Location, please investigate.");
                return;
            }
            Path schemaPath = Paths.get(schemaFile);
            loadSchemafile(schemaPath);
        } else {
            Path schemaPath = Paths.get(String.format("%s/%s", SRC_MAIN_RESOURCES, SCHEMA_FILE_NAME));
            loadSchemafile(schemaPath);
        }
    }

    /**
     * Validate if file already exists and populated. IT could be populated from the Dockerfile COPY statement into
     * the container. However, if not specified this will generate a new file for us.
     * @return
     */
    private String validateDockerOutputFileLocation() {
        String schemaFile = String.format("%s/%s", DOCKER_OUTPUT_PATH, SCHEMA_FILE_NAME);
        boolean fileDoesExist = fileUtil.validateIfFileExists(schemaFile);
        if (!fileDoesExist) {
            Path dockerOutputDir = Path.of(DOCKER_OUTPUT_PATH);
            if (Files.notExists(dockerOutputDir)) {
                logger.info("{} directory does not exist, will be created.", DOCKER_OUTPUT_PATH);
                try {
                    Files.createDirectory(dockerOutputDir);
                } catch (IOException e) {
                    logger.error("IOException Error has occurred attempting to create directory {}. Please investigate",
                            dockerOutputDir);
                    logger.error(e);
                    return null;
                }
            }
            try {
                Files.createFile(Paths.get(schemaFile));
            } catch (IOException e) {
                logger.error("IOException Error has occurred attempting to create file {}. Please investigate",
                        schemaFile);
                logger.error(e);
                return null;
            }
        }
        return schemaFile;
    }

    /**
     * Execute the defined FileSystemResource as specified by the Path passed in.
     * @param schemaPath
     */
    private void loadSchemafile(Path schemaPath) {
        if (Files.exists(schemaPath)) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new FileSystemResource(schemaPath.toFile()));
            populator.execute(dataSource);
        } else {
            logger.error("Error has occurred, Schema.sql not found at {}", schemaPath);
        }
    }
}
