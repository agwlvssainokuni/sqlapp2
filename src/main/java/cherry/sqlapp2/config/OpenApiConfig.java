/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cherry.sqlapp2.config;

import io.swagger.v3.oas.models.Components;
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

/**
 * OpenAPI/Swagger configuration for API documentation generation
 */
@Configuration
public class OpenApiConfig {

    private final String appTitle;
    private final String appVersion;
    private final String appDescription;

    public OpenApiConfig(
            @Value("${app.title:SqlApp2}") String appTitle,
            @Value("${app.version:0.0.0}") String appVersion,
            @Value("${app.description:Web-based SQL execution tool}") String appDescription
    ) {
        this.appTitle = appTitle;
        this.appVersion = appVersion;
        this.appDescription = appDescription;
    }

    @Bean
    public OpenAPI sqlApp2OpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appTitle)
                        .version(appVersion)
                        .description(appDescription)
                        .contact(new Contact()
                                .name("SqlApp2 Development Team")
                                .email("sqlapp2@example.com"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development server"),
                        new Server().url("https://sqlapp2.example.com").description("Production server")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from /api/auth/login endpoint")));
    }
}