package stock.market.data.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import stock.market.data.configuration.DockerDetectionConfiguration;

@Component
public class DataLoader implements CommandLineRunner {
    private final Logger logger = LogManager.getLogger(DataLoader.class);
    private final String API_URL = "https://www.alphavantage.co/query?function=TOP_GAINERS_LOSERS&apikey=demo";
    private final DockerDetectionConfiguration dockerDetectionConfiguration;

    public DataLoader(DockerDetectionConfiguration dockerDetectionConfiguration) {
        this.dockerDetectionConfiguration = dockerDetectionConfiguration;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.trace("Data Loading beginning.");

        String outputFile = "src/main/resources/data.sql";
        RestTemplate restTemplate = new RestTemplate();
        String jsonResponse = restTemplate.getForObject(API_URL, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = null;

        if (dockerDetectionConfiguration.isRunningInDocker()) {
            logger.info("We are currently executing inside of a docker container - Output file path will be modified.");
            outputFile = "/app/resources/data.sql";

            // Validate file already exists and populated.
            File f = new File(outputFile);
            if (f.exists() && f.isFile() && f.length() > 0){
                logger.info("{} already exists and is populated with data, no need to create.",  outputFile);
                return;
            }
        }

        try {
            responseMap = objectMapper.readValue(jsonResponse, Map.class);
        } catch (JsonProcessingException e) {
            logger.error("An exception has occurred attempted to Map the TOP_GAINERS_LOSERS. Please investigate.");
            throw new RuntimeException(e);
        }
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("DROP TABLE IF EXISTS top_gainers;\n");
            writer.write("CREATE TABLE top_gainers (ticker VARCHAR(10), price DECIMAL(10,2), change_amount DECIMAL(10,2), change_percentage DECIMAL(5,2));\n\n");

            writer.write("DROP TABLE IF EXISTS top_losers;\n");
            writer.write("CREATE TABLE top_losers (ticker VARCHAR(10), price DECIMAL(10,2), change_amount DECIMAL(10,2), change_percentage DECIMAL(5,2));\n\n");

            List<Map<String, String>> topGainers = (List<Map<String, String>>) responseMap.get("top_gainers");
            for (Map<String, String> gainer : topGainers) {
                String sql = String.format("INSERT INTO top_gainers (ticker, price, change_amount, change_percentage) VALUES ('%s', %s, %s, %s);\n",
                        gainer.get("ticker"),
                        gainer.get("price"),
                        gainer.get("change_amount"),
                        gainer.get("change_percentage").replace("%", ""));
                writer.write(sql);
            }

            List<Map<String, String>> topLosers = (List<Map<String, String>>) responseMap.get("top_losers");
            for (Map<String, String> loser : topLosers) {
                String sql = String.format("INSERT INTO top_losers (ticker, price, change_amount, change_percentage) VALUES ('%s', %s, %s, %s);\n",
                        loser.get("ticker"),
                        loser.get("price"),
                        loser.get("change_amount"),
                        loser.get("change_percentage").replace("%", ""));
                writer.write(sql);
            }
        } catch (IOException e) {
            logger.error("An IOException attempting to write to: {}", outputFile);
            throw new RuntimeException(e);
        }
        logger.trace("Data Loading complete.");
        logger.info("Data file generated successfully: {}", outputFile);
    }
}
