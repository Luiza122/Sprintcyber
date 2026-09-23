package com.ford.fordretain.config;

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
public class SwaggerConfig {

    @Bean
    public OpenAPI fordRetainOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FordRetain API")
                        .description("API de Retencao Preditiva de Clientes Ford. Desenvolvido como parte do Challenge Ford FIAP 2026.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe FordRetain - FIAP")
                                .email("grupo@fiap.com.br"))
                        .license(new License()
                                .name("Uso Academico")
                                .url("https://www.fiap.com.br")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT obtido no endpoint /auth/login")));
    }
}