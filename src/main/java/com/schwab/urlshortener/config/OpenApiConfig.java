package com.schwab.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Production URL Shortener API")
                        .version("1.0.0")
                        .description("High-performance, secure URL shortener service built with Java 17 and Spring Boot 3.x.")
                        .contact(new Contact()
                                .name("Engineering Team")
                                .email("engineering@example.com")));
    }
}
