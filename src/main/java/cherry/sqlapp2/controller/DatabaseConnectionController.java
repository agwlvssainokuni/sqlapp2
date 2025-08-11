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

import cherry.sqlapp2.dto.ConnectionCount;
import cherry.sqlapp2.dto.ConnectionStatus;
import cherry.sqlapp2.dto.ConnectionTestResult;
import cherry.sqlapp2.dto.DatabaseConnectionRequest;
import cherry.sqlapp2.dto.DatabaseConnection;
import cherry.sqlapp2.dto.DatabaseType;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.DatabaseConnectionService;
import cherry.sqlapp2.service.DynamicDataSourceService;
import cherry.sqlapp2.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/connections")
public class DatabaseConnectionController {

    private final DatabaseConnectionService connectionService;
    private final DynamicDataSourceService dynamicDataSourceService;
    private final UserService userService;

    @Autowired
    public DatabaseConnectionController(DatabaseConnectionService connectionService, 
                                      DynamicDataSourceService dynamicDataSourceService,
                                      UserService userService) {
        this.connectionService = connectionService;
        this.dynamicDataSourceService = dynamicDataSourceService;
        this.userService = userService;
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

    @GetMapping
    public ResponseEntity<List<DatabaseConnection>> getAllConnections(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        try {
            User currentUser = getCurrentUser();
            List<DatabaseConnection> connections;
            
            if (activeOnly) {
                connections = connectionService.getActiveConnectionsByUser(currentUser);
            } else {
                connections = connectionService.getAllConnectionsByUser(currentUser);
            }
            
            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatabaseConnection> getConnectionById(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            return connectionService.getConnectionById(currentUser, id)
                    .map(connection -> ResponseEntity.ok(connection))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createConnection(@Valid @RequestBody DatabaseConnectionRequest request) {
        try {
            // 新規作成時はパスワードが必須
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Connection creation failed: Password is required");
            }
            
            User currentUser = getCurrentUser();
            DatabaseConnection response = connectionService.createConnection(currentUser, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Connection creation failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Connection creation failed: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateConnection(@PathVariable Long id, @Valid @RequestBody DatabaseConnectionRequest request) {
        try {
            User currentUser = getCurrentUser();
            DatabaseConnection response = connectionService.updateConnection(currentUser, id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Connection update failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Connection update failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConnection(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            connectionService.deleteConnection(currentUser, id);
            return ResponseEntity.ok().body("Connection deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Connection deletion failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Connection deletion failed: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleConnectionStatus(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            connectionService.toggleConnectionStatus(currentUser, id);
            return ResponseEntity.ok().body("Connection status toggled successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Status toggle failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Status toggle failed: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ConnectionCount> getConnectionCount() {
        try {
            User currentUser = getCurrentUser();
            long activeCount = connectionService.getActiveConnectionCount(currentUser);
            
            ConnectionCount response = new ConnectionCount(activeCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<DatabaseType>> getDatabaseTypes() {
        try {
            List<DatabaseType> types = Arrays.stream(cherry.sqlapp2.enums.DatabaseType.values())
                    .map(type -> new DatabaseType(
                            type.name(),
                            type.getDisplayName(),
                            type.getDefaultPort()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-type/{databaseType}")
    public ResponseEntity<List<DatabaseConnection>> getConnectionsByType(@PathVariable String databaseType) {
        try {
            User currentUser = getCurrentUser();
            cherry.sqlapp2.enums.DatabaseType type = cherry.sqlapp2.enums.DatabaseType.fromString(databaseType);
            if (type == null) {
                return ResponseEntity.badRequest().build();
            }
            
            List<DatabaseConnection> connections = connectionService.getConnectionsByType(currentUser, type);
            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<ConnectionTestResult> testConnection(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            ConnectionTestResult result = connectionService.testConnection(currentUser, id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ConnectionTestResult failureResult = ConnectionTestResult.failure("Error testing connection: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failureResult);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<ConnectionTestResult> testConnectionRequest(@Valid @RequestBody DatabaseConnectionRequest request) {
        try {
            ConnectionTestResult result = connectionService.testConnection(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ConnectionTestResult failureResult = ConnectionTestResult.failure("Error testing connection: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failureResult);
        }
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<?> getConnectionInfo(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Map<String, Object> info = dynamicDataSourceService.getConnectionInfo(currentUser, id);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Connection info retrieval failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Connection info retrieval failed: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ConnectionStatus> getConnectionStatus(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            boolean available = dynamicDataSourceService.isConnectionAvailable(currentUser, id);
            
            ConnectionStatus response = new ConnectionStatus(
                    id, available, LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ConnectionStatus response = new ConnectionStatus(
                    id, false, e.getMessage(), LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}