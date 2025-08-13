/*
 * Copyright 2025 agwlvssainokuni
 *
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

package cherry.sqlapp2.controller;

import cherry.sqlapp2.dto.ExportRequest;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.ExportService;
import cherry.sqlapp2.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/export")
@Tag(name = "Export", description = "SQL query result export operations")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private UserService userService;

    private User getCurrentUser(Authentication authentication) {
        return Optional.of(authentication)
                .map(Authentication::getName)
                .flatMap(userService::findByUsername)
                .get();
    }

    @Operation(
            summary = "Export SQL query result",
            description = "Execute SQL query and export results in CSV or TSV format",
            requestBody = @RequestBody(
                    description = "Export request with SQL query, connection, and format",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "CSV Export",
                                            value = """
                                                    {
                                                        "sql": "SELECT id, name FROM users WHERE active = :active",
                                                        "connectionId": 1,
                                                        "format": "CSV",
                                                        "filename": "active_users.csv",
                                                        "parameters": {
                                                            "active": true
                                                        }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "TSV Export",
                                            value = """
                                                    {
                                                        "sql": "SELECT * FROM products WHERE category = :category",
                                                        "connectionId": 2,
                                                        "format": "TSV",
                                                        "filename": "electronics_products.tsv",
                                                        "parameters": {
                                                            "category": "Electronics"
                                                        }
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Export successful - returns file content",
                    content = @Content(mediaType = "text/csv")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or SQL syntax error"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Database connection not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "SQL execution error or export failed"
            )
    })
    @PostMapping
    public ResponseEntity<byte[]> exportSqlResult(
            @Valid @RequestBody ExportRequestWithParameters request,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);

        return exportService.exportSqlResult(
                request.getSql(),
                request.getParameters(),
                request.getConnectionId(),
                request.getFormat(),
                request.getFilename(),
                user
        );
    }

    /**
     * Extended export request that includes parameters
     */
    public static class ExportRequestWithParameters extends ExportRequest {
        private Map<String, Object> parameters;

        public ExportRequestWithParameters() {
            super();
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}