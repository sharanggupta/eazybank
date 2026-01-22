package dev.sharanggupta.account;

import dev.sharanggupta.account.config.AuditorAwareImpl;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@OpenAPIDefinition(
		info = @Info(
				title = "Account microservice REST API documentation",
				summary = "EazyBank Account microservice REST API documentation",
				version = "1.0.0",
				contact = @Contact(
						name = "Sharang Gupta",
						email = "sharang@sharanggupta.dev",
						url = "https://sharanggupta.dev"
				),
				license = @License(
						name = "Apache 2.0",
						url = "http://www.apache.org/licenses/LICENSE-2.0"
				)
		),
		externalDocs = @ExternalDocumentation(
				description = "Account microservice REST API documentation",
				url = "https://sharanggupta.dev"
		)
)
public class AccountApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccountApplication.class, args);
	}

	@Bean
	public AuditorAware<String> auditorAware() {
		return new AuditorAwareImpl();
	}
}
