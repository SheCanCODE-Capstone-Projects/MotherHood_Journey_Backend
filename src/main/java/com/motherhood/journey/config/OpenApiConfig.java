package com.motherhood.journey.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Value("${spring.profiles.active:local}")
    private String activeProfiles;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(buildInfo())
            .servers(buildServers())
            .externalDocs(new ExternalDocumentation()
                .description("MotherHood Journey — Project Repository")
                .url("https://github.com/SheCanCODE-Capstone-Projects/MotherHood_Journey_Backend"))
            .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                    .name(SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description(
                        "JWT access token obtained from **POST /api/v1/auth/login**. "
                        + "Paste the `accessToken` value here (without the 'Bearer ' prefix — Swagger adds it). "
                        + "Tokens expire after 24 hours; use **POST /api/v1/auth/refresh** to rotate."
                    )));
    }

    private Info buildInfo() {
        return new Info()
            .title("MotherHood Journey API")
            .version("1.0.0")
            .description(
                "## MotherHood Journey — Maternal & Child Health Platform\n\n"
                + "REST API for Rwanda's maternal and child health tracking system. "
                + "Covers mother registration, pregnancy monitoring, child growth, "
                + "vaccinations, health visits, government integration (NIDA / Irembo / HMIS), "
                + "and SMS notifications.\n\n"
                + "### Authentication\n"
                + "All endpoints except `GET /api/v1/geo/**`, `POST /api/v1/auth/**`, "
                + "and `GET /actuator/health` require a **Bearer JWT** in the `Authorization` header.\n\n"
                + "### Roles\n"
                + "| Role | Access |\n"
                + "|---|---|\n"
                + "| `HEALTH_WORKER` | Own-facility mothers, pregnancies, visits, children |\n"
                + "| `FACILITY_ADMIN` | All records within their facility |\n"
                + "| `DISTRICT_OFFICER` | Records across their district geo scope |\n"
                + "| `GOVERNMENT_ANALYST` | Read-only cross-facility analytics |\n"
                + "| `MOH_ADMIN` | Full platform access + admin operations |\n\n"
                + "### Mock Gov APIs (prod,mock profile)\n"
                + "When deployed with profile `mock`, NIDA / Irembo / HMIS are simulated "
                + "by embedded controllers at `/mock/nida`, `/mock/irembo`, `/mock/hmis`. "
                + "National IDs starting with **1 or 2** → VERIFIED. Starting with **3** → PARTIAL_MATCH.\n\n"
                + "### Test Credentials\n"
                + "Register a user via `POST /api/v1/auth/register`, then login. "
                + "Seed test mothers are available with password **Test@1234** "
                + "and phone numbers `+25078XXXXXXX` (X = 0000001–0001500)."
            )
            .contact(new Contact()
                .name("SheCanCODE Capstone Team")
                .email("wideskillsrw@gmail.com")
                .url("https://github.com/SheCanCODE-Capstone-Projects"))
            .license(new License()
                .name("Private — SheCanCODE Capstone 2024")
                .url("https://github.com/SheCanCODE-Capstone-Projects"));
    }

    private List<Server> buildServers() {
        Server production = new Server()
            .url("https://motherhoodjourneybackend-production.up.railway.app")
            .description("Production (Railway)");

        Server local = new Server()
            .url("http://localhost:8080")
            .description("Local development");

        // Put production first when the prod profile is active
        if (activeProfiles.contains("prod")) {
            return List.of(production, local);
        }
        return List.of(local, production);
    }
}
