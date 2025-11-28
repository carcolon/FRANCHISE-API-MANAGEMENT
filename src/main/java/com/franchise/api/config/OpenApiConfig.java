package com.franchise.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI franchiseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Franchise API")
                        .description("API para gestion de franquicias, sucursales y productos")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Carlos")
                                .email("cfca5@hotmail.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repositorio del proyecto")
                        .url("https://github.com/carcolon/FRANCHISE-API-MANAGEMENT/"));
    }
}
