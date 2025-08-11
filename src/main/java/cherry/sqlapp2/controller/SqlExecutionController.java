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

import cherry.sqlapp2.dto.ApiResponse;
import cherry.sqlapp2.dto.SqlExecutionRequest;
import cherry.sqlapp2.dto.SqlExecutionResult;
import cherry.sqlapp2.dto.SqlValidationResult;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.QueryManagementService;
import cherry.sqlapp2.service.SqlExecutionService;
import cherry.sqlapp2.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/sql")
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

    private User getCurrentUser() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .flatMap(userService::findByUsername)
                .get();
    }

    @PostMapping("/execute")
    public ApiResponse<SqlExecutionResult> executeQuery(
            @Valid @RequestBody SqlExecutionRequest request
    ) {
        User currentUser = getCurrentUser();

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

        // Check if this is a parameterized query
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            result = sqlExecutionService.executeParameterizedQuery(
                    currentUser,
                    request.getConnectionId(),
                    request.getSql(),
                    request.getParameters(),
                    request.getParameterTypes(),
                    savedQuery
            );
        } else {
            result = sqlExecutionService.executeQuery(
                    currentUser,
                    request.getConnectionId(),
                    request.getSql(),
                    savedQuery
            );
        }

        return ApiResponse.success(result);
    }

    @PostMapping("/validate")
    public ApiResponse<SqlValidationResult> validateQuery(@Valid @RequestBody SqlExecutionRequest request) {
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
