package com.apex.wallet.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI digitalWalletOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Wallet Engine — Core Ledger & Payment Processing API")
                        .description("Motor bancario e transacional de alta velocidade em Java 17 e Spring Boot 3 com suporte a partidas dobradas (Double-Entry Ledger), esteira antifraude/AML e degradacao graciosa sob contingencia.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mickael Geronimo")
                                .url("https://github.com/MickaelGeronimo/wallet-core-service"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT gerado via POST /api/auth/login")));
    }
}