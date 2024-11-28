package stock.market.data.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stock.market.data.utility.DataLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class DockerDetectionConfiguration {
    private final Logger logger = LogManager.getLogger(DataLoader.class);

    @Bean
    public boolean isRunningInDocker() {
        try {
            return Files.exists(Paths.get("/.dockerenv")) ||
                    Files.readString(Paths.get("/proc/1/cgroup")).contains("/docker");
        } catch (IOException e) {
            logger.error("An error has occurred attempting to detect the docker environment file. " +
                    "We are not currently running in Docker.");
            return false;
        }
    }
}
