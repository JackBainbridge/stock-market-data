package stock.market.data.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import stock.market.data.configuration.DockerDetectionConfiguration;
import stock.market.data.constants.AlphaVantageAPIFunctionConstants;

@Component
public class DataLoader implements CommandLineRunner {
    private final Logger logger = LogManager.getLogger(DataLoader.class);

    // Define constants for the API URL, parameters and output file
    private static final String API_BASE_URL = "https://www.alphavantage.co/query?";
    private static final String FUNCTION_PARAM = "function=%s&apikey=%s";
    private static final String API_KEY = "demo"; // Ideally, this should be retrieved from a secure source
    private static final String OUTPUT_LOCAL_PATH = "src/main/resources";
    private static final String DOCKER_OUTPUT_PATH = "/app/resources";
    private static final String OUTPUT_FILE_NAME = "data.sql";

    private final DockerDetectionConfiguration dockerDetectionConfiguration;
    private final DataSource dataSource;

    public DataLoader(DockerDetectionConfiguration dockerDetectionConfiguration, DataSource dataSource) {
        this.dockerDetectionConfiguration = dockerDetectionConfiguration;
        this.dataSource = dataSource;
    }

    /**
     * Method executed at app creation time. Determines if we are local or on docker, and generates a file accordingly,
     * then loads the data.sql file into in memory H2 Database.
     * @param args
     */
    @Override
    public void run(String... args) {
        logger.trace("Data Loading beginning.");

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        String outputFile = String.format("%s/%s", OUTPUT_LOCAL_PATH, OUTPUT_FILE_NAME);

        if (dockerDetectionConfiguration.isRunningInDocker()) {
            logger.info("We are currently executing inside of a docker container - Output file path will be modified.");
            outputFile = validateDockerOutputFileLocation();
            if (outputFile == null) {
                logger.error("Unable to validate Docker Output File Location, please investigate.");
                return;
            }
        }
        boolean dataRetrieved = retrieveTopGainersAndLosersAndTraded(objectMapper, restTemplate, outputFile);
        if (!dataRetrieved) {
            logger.error("Error has occurred! Data has NOT been retrieved via method: " +
                    "retrieveTopGainersAndLosersAndTraded. Investigate.");
            return;
        }

        boolean loadComplete = loadTheGeneratedDataFile(outputFile);

        if (!loadComplete) {
            logger.error("Error has occurred! Data has NOT been loaded. Investigate.");
            return;
        }
        logger.trace("Data Loading complete.");
        logger.info("Data Loading complete.");
    }

    /**
     * Validate file already exists and populated. We validate file first as it should be populated from the
     * Dockerfile COPY statement. However, if not specified this will generate a new file for us.
     * @return
     */
    private String validateDockerOutputFileLocation() {
        String outputFile = String.format("%s/%s", DOCKER_OUTPUT_PATH, OUTPUT_FILE_NAME);
        File f = new File(outputFile);
        if (!f.exists()) {
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
                Files.createFile(Paths.get(outputFile));
            } catch (IOException e) {
                logger.error("IOException Error has occurred attempting to create file {}. Please investigate",
                        outputFile);
                logger.error(e);
                return null;
            }
        } else if (f.exists() && f.isFile() && f.length() > 0){
            logger.info("{} already exists and is populated with data, no need to create.",  outputFile);
            return outputFile;
        }
        return outputFile;
    }

    /**
     * Retrieves Top Gainers, Losers and Actively Traded stocks and writes them to outputFile.
     * @param objectMapper
     * @param restTemplate
     * @param outputFile
     * @return
     */
    private boolean retrieveTopGainersAndLosersAndTraded(ObjectMapper objectMapper, RestTemplate restTemplate, String outputFile) {
        // Construct the full API URL.
        String apiFullUrl = String.format(API_BASE_URL + FUNCTION_PARAM,
                AlphaVantageAPIFunctionConstants.TOP_GAINERS_LOSERS.name(), API_KEY);

        String jsonResponse;
        try {
            jsonResponse = restTemplate.getForObject(apiFullUrl, String.class);
        } catch (Exception e) {
            logger.error("Error has occurred attempt to retrieve data : {}. Please investigate.", apiFullUrl);
            return false;
        }

        Map<String, Object> responseMap;
        try {
            // Use Jackson's TypeReference to safely deserialize the JSON response into the desired type.
            responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            logger.error("An exception has occurred attempted to Map the TOP_GAINERS_LOSERS. Please investigate.");
            throw new RuntimeException(e);
        }
        try (FileWriter writer = new FileWriter(outputFile)) {
            List<Map<String, String>> topGainers = objectMapper.convertValue(responseMap.get("top_gainers"),
                    new TypeReference<>() {});
            for (Map<String, String> gainer : topGainers) {
                String sql = String.format("INSERT INTO top_gainers (ticker, price, change_amount, change_percentage) VALUES ('%s', %s, %s, %s);\n",
                        gainer.get("ticker"),
                        gainer.get("price"),
                        gainer.get("change_amount"),
                        gainer.get("change_percentage").replace("%", ""));
                writer.write(sql);
            }

            List<Map<String, String>> topLosers = objectMapper.convertValue(responseMap.get("top_losers"),
                    new TypeReference<>() {});
            for (Map<String, String> loser : topLosers) {
                String sql = String.format("INSERT INTO top_losers (ticker, price, change_amount, change_percentage) VALUES ('%s', %s, %s, %s);\n",
                        loser.get("ticker"),
                        loser.get("price"),
                        loser.get("change_amount"),
                        loser.get("change_percentage").replace("%", ""));
                writer.write(sql);
            }

            List<Map<String, String>> mostActivelyTraded = objectMapper.convertValue(responseMap.get("most_actively_traded"),
                    new TypeReference<>() {});
            for (Map<String, String> stock : mostActivelyTraded) {
                String sql = String.format("INSERT INTO most_actively_traded (ticker, price, change_amount, change_percentage, volume) VALUES ('%s', '%s', %s, %s, %s);\n",
                        stock.get("ticker"),
                        stock.get("price"),
                        stock.get("change_amount"),
                        stock.get("change_percentage").replace("%", ""),
                        stock.get("volume"));
                writer.write(sql);
            }
        } catch (IOException e) {
            logger.error("An IOException attempting to write to: {}", outputFile);
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * Load the generated data.sql file into the H2 database.
     * @param outputFile
     * @return
     */
    private boolean loadTheGeneratedDataFile(String outputFile) {
        File sqlFile = new File(outputFile);
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
        } catch (IOException e) {
            logger.error("Error reading the SQL file: {}", e.getMessage());
        }

        return true;
    }
}
