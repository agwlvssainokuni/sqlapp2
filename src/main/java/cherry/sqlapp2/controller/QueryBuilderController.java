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

import cherry.sqlapp2.dto.ApiResponse;
import cherry.sqlapp2.dto.QueryBuilderRequest;
import cherry.sqlapp2.dto.QueryBuilderResponse;
import cherry.sqlapp2.service.QueryBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query-builder")
@Tag(name = "Query Builder", description = "Visual SQL query building operations")
@SecurityRequirement(name = "bearerAuth")
public class QueryBuilderController {

    @Autowired
    private QueryBuilderService queryBuilderService;

    @Operation(
            summary = "Build SQL query from structure",
            description = "Generate SQL query from structured query components (SELECT, FROM, WHERE, etc.)",
            requestBody = @RequestBody(
                    description = "Query structure with SELECT, FROM, WHERE, and other SQL clauses",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "selectClause": "id, name, email",
                                                "fromClause": "users",
                                                "whereClause": "active = true AND created_date > :startDate",
                                                "orderByClause": "created_date DESC",
                                                "limitClause": "50"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SQL query built successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid query structure or syntax error"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @PostMapping("/build")
    public ApiResponse<QueryBuilderResponse> buildQuery(
            @Valid @RequestBody QueryBuilderRequest request,
            Authentication authentication) {

        QueryBuilderResponse response = queryBuilderService.buildQuery(request);
        return ApiResponse.success(response);
    }

}