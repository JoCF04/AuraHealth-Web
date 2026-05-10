package com.aurahealth.api.auraconfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuraSwaggerConfig {

    @Bean
    public OpenAPI auraOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AuraHealth API")
                        .description("Plataforma de Salud Preventiva — Backend RESTful con Spring Boot")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo AuraHealth")
                                .email("aurahealth@upc.edu.pe")));
    }
}
