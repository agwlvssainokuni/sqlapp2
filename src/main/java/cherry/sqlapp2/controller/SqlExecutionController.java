/*
 * Copyright 2025 SqlApp2
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

import cherry.sqlapp2.dto.*;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.QueryManagementService;
import cherry.sqlapp2.service.SqlExecutionService;
import cherry.sqlapp2.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * SQL実行機能を提供するコントローラクラス。
 * SQLクエリの実行、バリデーション、結果の取得など、
 * SQL実行に関連する機能を提供します。
 */
@RestController
@RequestMapping("/api/sql")
@Tag(name = "SQL Execution", description = "SQL query execution and validation operations")
public class SqlExecutionController {

    private final SqlExecutionService sqlExecutionService;
    private final UserService userService;
    private final QueryManagementService queryManagementService;

    @Autowired
    public SqlExecutionController(
            SqlExecutionService sqlExecutionService,
            UserService userService,
            QueryManagementService queryManagementService
    ) {
        this.sqlExecutionService = sqlExecutionService;
        this.userService = userService;
        this.queryManagementService = queryManagementService;
    }

    private User getCurrentUser(Authentication authentication) {
        return Optional.of(authentication)
                .map(Authentication::getName)
                .flatMap(userService::findByUsername)
                .get();
    }

    /**
     * SQLクエリを実行します。
     * パラメータ付きクエリの実行、ページネーション、結果の履歴記録を行います。
     *
     * @param request        SQL実行リクエスト（SQL文、接続ID、パラメータなど）
     * @param authentication 認証情報
     * @return SQL実行結果を含むAPIレスポンス
     */
    @Operation(
            summary = "Execute SQL query",
            description = "Execute SQL query with optional parameters and pagination support",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "SQL execution request with query, connection, and optional parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SqlExecutionRequest.class),
                            examples = @ExampleObject(
                                    name = "SQL Execution Example",
                                    value = "{\"sql\": \"SELECT * FROM users WHERE id = :userId\", \"connectionId\": 1, \"parameters\": {\"userId\": \"123\"}, \"parameterTypes\": {\"userId\": \"INTEGER\"}}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SQL query executed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid SQL query or parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/execute")
    public ApiResponse<SqlExecutionResult> executeQuery(
            @Valid @RequestBody SqlExecutionRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        // Validate SQL first
        try {
            sqlExecutionService.validateQuery(request.getSql());
        } catch (IllegalArgumentException e) {
            return ApiResponse.success(
                    SqlExecutionResult.validationNg(
                            LocalDateTime.now(), request.getSql(), e.getMessage(), "ValidationError"
                    )
            );
        }

        // If validation only, return success without execution
        if (request.isValidateOnly()) {
            return ApiResponse.success(
                    SqlExecutionResult.validationOk(
                            LocalDateTime.now(), request.getSql()
                    )
            );
        }

        // Execute the query
        SqlExecutionResult result;

        // Get SavedQuery if savedQueryId is provided
        SavedQuery savedQuery = Optional.ofNullable(request.getSavedQueryId())
                .flatMap(savedQueryId ->
                        queryManagementService.getAccessibleQuery(savedQueryId, currentUser)
                ).orElse(null);

        // Extract pagination request if provided
        PagingRequest pagingRequest = null;
        if (request.getPagingRequest() != null) {
            pagingRequest = request.getPagingRequest();
        }

        // Check if this is a parameterized query
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            result = sqlExecutionService.executeParameterizedQuery(
                    currentUser,
                    request.getConnectionId(),
                    request.getSql(),
                    request.getParameters(),
                    request.getParameterTypes(),
                    savedQuery,
                    pagingRequest
            );
        } else {
            result = sqlExecutionService.executeQuery(
                    currentUser,
                    request.getConnectionId(),
                    request.getSql(),
                    savedQuery,
                    pagingRequest
            );
        }

        return ApiResponse.success(result);
    }

    @Operation(
            summary = "Validate SQL query",
            description = "Validate SQL query syntax without execution",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "SQL validation request with query to validate",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SqlExecutionRequest.class),
                            examples = @ExampleObject(
                                    name = "SQL Validation Example",
                                    value = "{\"sql\": \"SELECT * FROM users WHERE id = :userId\", \"validateOnly\": true}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SQL validation completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @PostMapping("/validate")
    public ApiResponse<SqlValidationResult> validateQuery(
            @Valid @RequestBody SqlExecutionRequest request
    ) {
        // Validate SQL first
        try {
            sqlExecutionService.validateQuery(request.getSql());
        } catch (IllegalArgumentException e) {
            return ApiResponse.success(
                    SqlValidationResult.validationNg(
                            LocalDateTime.now(), request.getSql(), e.getMessage(), "ValidationError"
                    )
            );
        }

        return ApiResponse.success(
                SqlValidationResult.validationOk(
                        LocalDateTime.now(), request.getSql()
                )
        );
    }
}
