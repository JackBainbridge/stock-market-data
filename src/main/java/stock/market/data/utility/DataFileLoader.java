package stock.market.data.utility;

import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import stock.market.data.configuration.DockerDetectionConfiguration;

@Component
@Order(4)
public class DataFileLoader implements CommandLineRunner {
    private final Logger logger = LogManager.getLogger(DataFileLoader.class);

    @Value("${src.main.resources}")
    private String SRC_MAIN_RESOURCES;

    @Value("${data.filename}")
    private String DATA_FILE_NAME;

    @Value("${output.docker.path}")
    private String DOCKER_OUTPUT_PATH;

    private final DataSource dataSource;
    private final DockerDetectionConfiguration dockerDetectionConfiguration;

    public DataFileLoader(DockerDetectionConfiguration dockerDetectionConfiguration, DataSource dataSource,
                          DockerDetectionConfiguration dockerDetectionConfiguration1) {
        this.dataSource = dataSource;
        this.dockerDetectionConfiguration = dockerDetectionConfiguration1;
    }

    /**
     * Method executed at app creation time. Determines if we are local or on docker, and generates a file accordingly,
     * then loads the data.sql file into in memory H2 Database.
     * @param args
     */
    @Override
    public void run(String... args) {
        logger.trace("Data Loading beginning.");
        String dataFile;
        if (dockerDetectionConfiguration.isRunningInDocker()) {
            dataFile = String.format("%s/%s", DOCKER_OUTPUT_PATH, DATA_FILE_NAME);
        } else {
            dataFile = String.format("%s/%s", SRC_MAIN_RESOURCES, DATA_FILE_NAME);
        }

        boolean loadComplete = loadTheGeneratedDataFile(dataFile);
        if (!loadComplete) {
            logger.error("Error has occurred! Data has NOT been loaded. Investigate.");
            return;
        }
        logger.trace("Data Loading complete.");
        logger.info("Data Loading complete.");
    }

    /**
     * Load the generated data.sql file into the H2 database.
     * @param dataFile
     * @return
     */
    private boolean loadTheGeneratedDataFile(String dataFile) {
        File sqlFile = new File(dataFile);
        if (!sqlFile.exists()) {
            System.out.println("data.sql file not found!");
            return false;
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    statement.execute(line);
                }
            }
        } catch (SQLException e) {
            logger.error("SQL error occurred while loading data: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            logger.error("Error reading the SQL file: {}", e.getMessage());
            return false;
        }

        return true;
    }
}
