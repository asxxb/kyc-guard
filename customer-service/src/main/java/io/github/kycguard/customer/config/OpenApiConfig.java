package io.github.kycguard.customer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KYC-Guard Customer Service API")
                        .description("Customer onboarding, master data management, and transactional outbox event publishing")
                        .version("v1.0.0")
                        .contact(new Contact().name("KYC-Guard Compliance Engineering")));
    }
}
