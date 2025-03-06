package stock.market.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class DataApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(DataApplication.class, args);

		// Add a shutdown hook
		context.registerShutdownHook();
	}
}