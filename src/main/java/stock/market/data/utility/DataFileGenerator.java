package stock.market.data.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import stock.market.data.configuration.DockerDetectionConfiguration;
import stock.market.data.constants.AlphaVantageAPIFunctionConstants;
import stock.market.data.events.RunnerCompletedEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class DataFileGenerator implements CommandLineRunner {
    private final Logger logger = LogManager.getLogger(DataFileGenerator.class);

    @Value("${api.base.url}")
    private String API_BASE_URL;

    @Value("${function.param}")
    private String FUNCTION_PARAM;

    @Value("${api.key}")
    private String API_KEY;

    @Value("${src.main.resources}")
    private String SRC_MAIN_RESOURCES;

    @Value("${output.docker.path}")
    private String DOCKER_OUTPUT_PATH;

    @Value("${data.filename}")
    private String DATA_FILE_NAME;

    private final DockerDetectionConfiguration dockerDetectionConfiguration;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ApplicationContext context;
    private final FileUtil fileUtil;

    public DataFileGenerator(DockerDetectionConfiguration dockerDetectionConfiguration,
                             ApplicationEventPublisher applicationEventPublisher,
                             ApplicationContext context,
                             FileUtil fileUtil) {
        this.dockerDetectionConfiguration = dockerDetectionConfiguration;
        this.applicationEventPublisher = applicationEventPublisher;
        this.context = context;
        this.fileUtil = fileUtil;
    }

    @Override
    public void run(String... args) {
        String dataFile = String.format("%s/%s", SRC_MAIN_RESOURCES, DATA_FILE_NAME);

        if (dockerDetectionConfiguration.isRunningInDocker()) {
            logger.info("We are currently executing inside of a docker container - Output file path will be modified.");
            dataFile = validateDockerOutputFileLocation();
            if (dataFile == null) {
                logger.error("Unable to validate Docker Output File Location, please investigate.");
                return;
            }
        }

        Map<String, Object> jsonResponse;
        boolean fileExists = fileUtil.validateIfFileExists(dataFile);
        if (fileExists) {
            applicationEventPublisher.publishEvent(new RunnerCompletedEvent(this, DataFileGenerator.class));
            return;
        } else {
            jsonResponse = retrieveTopGainersAndLosersAndTraded(dataFile);
            if (jsonResponse == null) {
                logger.info("retrieveTopGainersAndLosersAndTraded has returned NULL. Please Investigate.");

                // Shutdown the application
                ((ConfigurableApplicationContext) context).close();
                return;
            }
            if (jsonResponse.containsKey("Information")) {
                logger.info("We have reached the limit of demo API calls! {}", jsonResponse.get("Information"));

                // Shutdown the application
                ((ConfigurableApplicationContext) context).close();
                return;
            }
        }

        boolean sqlDataFileGenerated = writeDataFile(dataFile, jsonResponse);
        if (!sqlDataFileGenerated) {
            logger.info("Data.sql file was not generated. Please investigate.");

            // Shutdown the application
            ((ConfigurableApplicationContext) context).close();
            return;
        }
        applicationEventPublisher.publishEvent(new RunnerCompletedEvent(this, DataFileGenerator.class));
    }

    /**
     * Retrieves Top Gainers, Losers and Actively Traded stocks and writes them to dataFile.
     * @param dataFile
     * @return
     */
    private Map<String, Object> retrieveTopGainersAndLosersAndTraded(String dataFile) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        String apiFullUrl = String.format(API_BASE_URL + FUNCTION_PARAM,
                AlphaVantageAPIFunctionConstants.TOP_GAINERS_LOSERS.name(), API_KEY);

        String jsonResponse;
        try {
            jsonResponse = restTemplate.getForObject(apiFullUrl, String.class);
        } catch (Exception e) {
            logger.error("Error has occurred attempt to retrieve data : {}. Please investigate.", apiFullUrl);
            return null;
        }

        Map<String, Object> responseMap;
        try {
            // Use Jackson's TypeReference to safely deserialize the JSON response into the desired type.
            responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            logger.error("An exception has occurred attempted to Map the TOP_GAINERS_LOSERS. Please investigate.");
            return null;
        }
        return responseMap;
    }

    private boolean writeDataFile(String dataFile, Map<String, Object> responseMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileWriter writer = new FileWriter(dataFile)) {
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
            logger.error("An IOException attempting to write to: {} .. exception {}", dataFile, e);
            return false;
        }
        return true;
    }

    /**
     * Validate if file already exists and populated. IT could be populated from the Dockerfile COPY statement into
     * the container. However, if not specified this will generate a new file for us.
     * @return
     */
    private String validateDockerOutputFileLocation() {
        String dataFile = String.format("%s/%s", DOCKER_OUTPUT_PATH, DATA_FILE_NAME);
        File f = new File(dataFile);
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
                Files.createFile(Paths.get(dataFile));
            } catch (IOException e) {
                logger.error("IOException Error has occurred attempting to create file {}. Please investigate",
                        dataFile);
                logger.error(e);
                return null;
            }
        }
        return dataFile;
    }
}
