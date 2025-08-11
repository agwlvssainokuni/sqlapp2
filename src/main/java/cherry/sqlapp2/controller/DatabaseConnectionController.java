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
import cherry.sqlapp2.dto.ConnectionTestResult;
import cherry.sqlapp2.dto.DatabaseConnection;
import cherry.sqlapp2.dto.DatabaseConnectionRequest;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.DatabaseConnectionService;
import cherry.sqlapp2.service.DynamicDataSourceService;
import cherry.sqlapp2.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/connections")
public class DatabaseConnectionController {

    private final DatabaseConnectionService connectionService;
    private final DynamicDataSourceService dynamicDataSourceService;
    private final UserService userService;

    @Autowired
    public DatabaseConnectionController(
            DatabaseConnectionService connectionService,
            DynamicDataSourceService dynamicDataSourceService,
            UserService userService
    ) {
        this.connectionService = connectionService;
        this.dynamicDataSourceService = dynamicDataSourceService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .flatMap(userService::findByUsername)
                .get();
    }

    @GetMapping
    public ApiResponse<List<DatabaseConnection>> getAllConnections(
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        User currentUser = getCurrentUser();
        if (activeOnly) {
            return ApiResponse.success(connectionService.getActiveConnectionsByUser(currentUser));
        } else {
            return ApiResponse.success(connectionService.getAllConnectionsByUser(currentUser));
        }
    }

    @Deprecated
    @GetMapping("/{id}")
    public ApiResponse<DatabaseConnection> getConnectionById(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        return connectionService.getConnectionById(currentUser, id)
                .map(ApiResponse::success)
                .get();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DatabaseConnection>> createConnection(
            @Valid @RequestBody DatabaseConnectionRequest request
    ) {
        // 新規作成時はパスワードが必須
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.error(List.of("Connection creation failed: Password is required"))
            );
        }

        User currentUser = getCurrentUser();
        DatabaseConnection connection = connectionService.createConnection(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(connection)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<DatabaseConnection> updateConnection(
            @PathVariable Long id,
            @Valid @RequestBody DatabaseConnectionRequest request
    ) {
        User currentUser = getCurrentUser();
        DatabaseConnection connection = connectionService.updateConnection(currentUser, id, request);
        return ApiResponse.success(connection);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConnection(
            @PathVariable Long id
    ) {
        User currentUser = getCurrentUser();
        connectionService.deleteConnection(currentUser, id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<ConnectionTestResult> testConnection(
            @PathVariable Long id
    ) {
        User currentUser = getCurrentUser();
        ConnectionTestResult result = connectionService.testConnection(currentUser, id);
        return ApiResponse.success(result);
    }
}
