package dev.sharanggupta.customergateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CustomerGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerGatewayApplication.class, args);
	}

}
