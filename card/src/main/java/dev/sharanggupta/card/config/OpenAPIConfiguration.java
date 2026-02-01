package dev.sharanggupta.card.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfiguration {

    @Value("${app.build.version:1.0.0}")
    private String appVersion;

    @Value("${app.support.contact.name:Card Service Support Team}")
    private String contactName;

    @Value("${app.support.contact.email:support@card.service.local}")
    private String contactEmail;

    @Bean
    public OpenAPI cardServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Card Microservice REST API")
                        .summary("EazyBank Card Microservice REST API Documentation")
                        .version(appVersion)
                        .contact(new Contact()
                                .name(contactName)
                                .email(contactEmail))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")));
    }
}
