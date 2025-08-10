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

import cherry.sqlapp2.dto.SqlExecutionRequest;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.QueryManagementService;
import cherry.sqlapp2.service.SqlExecutionService;
import cherry.sqlapp2.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sql")
public class SqlExecutionController {

    private final SqlExecutionService sqlExecutionService;
    private final UserService userService;
    private final QueryManagementService queryManagementService;

    @Autowired
    public SqlExecutionController(SqlExecutionService sqlExecutionService, UserService userService, 
                                QueryManagementService queryManagementService) {
        this.sqlExecutionService = sqlExecutionService;
        this.userService = userService;
        this.queryManagementService = queryManagementService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeQuery(@Valid @RequestBody SqlExecutionRequest request) {
        try {
            User currentUser = getCurrentUser();
            
            // Validate SQL first
            sqlExecutionService.validateQuery(request.getSql());
            
            // If validation only, return success without execution
            if (request.isValidateOnly()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("valid", true);
                result.put("message", "SQL query is valid");
                result.put("validatedAt", LocalDateTime.now());
                return ResponseEntity.ok(result);
            }
            
            // Execute the query
            Map<String, Object> result;
            
            // Get SavedQuery if savedQueryId is provided
            SavedQuery savedQuery = null;
            if (request.getSavedQueryId() != null) {
                savedQuery = queryManagementService.getAccessibleQuery(request.getSavedQueryId(), currentUser)
                    .orElse(null);
            }
            
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
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", "ValidationError");
            errorResult.put("executedAt", LocalDateTime.now());
            
            return ResponseEntity.badRequest().body(errorResult);
            
        } catch (SQLException e) {
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", "SQLException");
            errorResult.put("sqlState", e.getSQLState());
            errorResult.put("errorCode", e.getErrorCode());
            errorResult.put("executedAt", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Unexpected error: " + e.getMessage());
            errorResult.put("errorType", "SystemError");
            errorResult.put("executedAt", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateQuery(@Valid @RequestBody SqlExecutionRequest request) {
        try {
            sqlExecutionService.validateQuery(request.getSql());
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("valid", true);
            result.put("message", "SQL query is valid");
            result.put("validatedAt", LocalDateTime.now());
            result.put("sql", request.getSql());
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("valid", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", "ValidationError");
            errorResult.put("validatedAt", LocalDateTime.now());
            errorResult.put("sql", request.getSql());
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
}