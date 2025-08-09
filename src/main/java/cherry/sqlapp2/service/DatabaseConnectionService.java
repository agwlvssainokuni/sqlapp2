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
package cherry.sqlapp2.service;

import cherry.sqlapp2.dto.ConnectionTestResult;
import cherry.sqlapp2.dto.DatabaseConnectionRequest;
import cherry.sqlapp2.dto.DatabaseConnectionResponse;
import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.enums.DatabaseType;
import cherry.sqlapp2.repository.DatabaseConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DatabaseConnectionService {

    private final DatabaseConnectionRepository connectionRepository;
    private final EncryptionService encryptionService;

    @Autowired
    public DatabaseConnectionService(DatabaseConnectionRepository connectionRepository, 
                                   EncryptionService encryptionService) {
        this.connectionRepository = connectionRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public List<DatabaseConnectionResponse> getAllConnectionsByUser(User user) {
        return connectionRepository.findByUserOrderByUpdatedAtDesc(user)
                .stream()
                .map(DatabaseConnectionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DatabaseConnectionResponse> getActiveConnectionsByUser(User user) {
        return connectionRepository.findByUserAndIsActiveOrderByUpdatedAtDesc(user, true)
                .stream()
                .map(DatabaseConnectionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<DatabaseConnectionResponse> getConnectionById(User user, Long connectionId) {
        return connectionRepository.findByUserAndId(user, connectionId)
                .map(DatabaseConnectionResponse::new);
    }

    @Transactional(readOnly = true)
    public Optional<DatabaseConnection> getConnectionEntityById(User user, Long connectionId) {
        return connectionRepository.findByUserAndId(user, connectionId);
    }

    @Transactional(readOnly = true)
    public boolean existsByConnectionName(User user, String connectionName) {
        return connectionRepository.existsByUserAndConnectionName(user, connectionName);
    }

    public DatabaseConnectionResponse createConnection(User user, DatabaseConnectionRequest request) {
        // Check if connection name already exists for this user
        if (connectionRepository.existsByUserAndConnectionName(user, request.getConnectionName())) {
            throw new IllegalArgumentException("Connection name already exists: " + request.getConnectionName());
        }

        // Use default port if not specified
        int port = request.getPort();
        if (port <= 0) {
            port = request.getDatabaseType().getDefaultPort();
        }

        // Create new connection entity
        DatabaseConnection connection = new DatabaseConnection(
                user,
                request.getConnectionName(),
                request.getDatabaseType(),
                request.getHost(),
                port,
                request.getDatabaseName(),
                request.getUsername()
        );

        // Encrypt and set password
        String encryptedPassword = encryptionService.encrypt(request.getPassword());
        connection.setEncryptedPassword(encryptedPassword);
        connection.setAdditionalParams(request.getAdditionalParams());
        connection.setActive(request.isActive());

        // Save and return response
        DatabaseConnection savedConnection = connectionRepository.save(connection);
        return new DatabaseConnectionResponse(savedConnection);
    }

    public DatabaseConnectionResponse updateConnection(User user, Long connectionId, DatabaseConnectionRequest request) {
        DatabaseConnection existingConnection = connectionRepository.findByUserAndId(user, connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

        // Check if new connection name conflicts with existing ones (excluding current)
        if (!existingConnection.getConnectionName().equals(request.getConnectionName())) {
            if (connectionRepository.existsByUserAndConnectionName(user, request.getConnectionName())) {
                throw new IllegalArgumentException("Connection name already exists: " + request.getConnectionName());
            }
        }

        // Use default port if not specified
        int port = request.getPort();
        if (port <= 0) {
            port = request.getDatabaseType().getDefaultPort();
        }

        // Update connection properties
        existingConnection.setConnectionName(request.getConnectionName());
        existingConnection.setDatabaseType(request.getDatabaseType());
        existingConnection.setHost(request.getHost());
        existingConnection.setPort(port);
        existingConnection.setDatabaseName(request.getDatabaseName());
        existingConnection.setUsername(request.getUsername());
        existingConnection.setAdditionalParams(request.getAdditionalParams());
        existingConnection.setActive(request.isActive());

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            String encryptedPassword = encryptionService.encrypt(request.getPassword());
            existingConnection.setEncryptedPassword(encryptedPassword);
        }

        // Save and return response
        DatabaseConnection updatedConnection = connectionRepository.save(existingConnection);
        return new DatabaseConnectionResponse(updatedConnection);
    }

    public void deleteConnection(User user, Long connectionId) {
        DatabaseConnection connection = connectionRepository.findByUserAndId(user, connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
        
        connectionRepository.delete(connection);
    }

    public void toggleConnectionStatus(User user, Long connectionId) {
        DatabaseConnection connection = connectionRepository.findByUserAndId(user, connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
        
        connection.setActive(!connection.isActive());
        connectionRepository.save(connection);
    }

    @Transactional(readOnly = true)
    public long getActiveConnectionCount(User user) {
        return connectionRepository.countActiveConnectionsByUser(user);
    }

    @Transactional(readOnly = true)
    public List<DatabaseConnectionResponse> getConnectionsByType(User user, DatabaseType databaseType) {
        return connectionRepository.findByUserAndDatabaseType(user, databaseType)
                .stream()
                .map(DatabaseConnectionResponse::new)
                .collect(Collectors.toList());
    }

    public String getDecryptedPassword(User user, Long connectionId) {
        DatabaseConnection connection = connectionRepository.findByUserAndId(user, connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
        
        return encryptionService.decrypt(connection.getEncryptedPassword());
    }

    public ConnectionTestResult testConnection(User user, Long connectionId) {
        try {
            DatabaseConnection connection = connectionRepository.findByUserAndId(user, connectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
            
            return testConnection(connection);
        } catch (IllegalArgumentException e) {
            return ConnectionTestResult.failure(e.getMessage());
        }
    }

    public ConnectionTestResult testConnection(DatabaseConnectionRequest request) {
        try {
            // Use default port if not specified
            int port = request.getPort();
            if (port <= 0) {
                port = request.getDatabaseType().getDefaultPort();
            }

            String baseUrl = request.getDatabaseType().buildUrl(
                    request.getHost(), 
                    port, 
                    request.getDatabaseName()
            );
            
            // Add additional parameters if provided
            String connectionUrl = baseUrl;
            if (request.getAdditionalParams() != null && !request.getAdditionalParams().trim().isEmpty()) {
                String params = request.getAdditionalParams().trim();
                if (!params.startsWith("?")) {
                    params = "?" + params;
                }
                connectionUrl = baseUrl + params;
            }

            try (Connection conn = DriverManager.getConnection(
                    connectionUrl, 
                    request.getUsername(), 
                    request.getPassword())) {
                
                // Test if connection is valid with 5 second timeout
                if (conn.isValid(5)) {
                    return ConnectionTestResult.success();
                } else {
                    return ConnectionTestResult.failure("Connection is not valid");
                }
                
            } catch (SQLException e) {
                return ConnectionTestResult.failure("Database connection failed: " + e.getMessage());
            }
        } catch (Exception e) {
            return ConnectionTestResult.failure("Unexpected error during connection test: " + e.getMessage());
        }
    }

    private ConnectionTestResult testConnection(DatabaseConnection connection) {
        try {
            String decryptedPassword = encryptionService.decrypt(connection.getEncryptedPassword());
            String connectionUrl = connection.buildJdbcUrl();

            try (Connection conn = DriverManager.getConnection(
                    connectionUrl, 
                    connection.getUsername(), 
                    decryptedPassword)) {
                
                // Test if connection is valid with 5 second timeout
                if (conn.isValid(5)) {
                    return ConnectionTestResult.success();
                } else {
                    return ConnectionTestResult.failure("Connection is not valid");
                }
                
            } catch (SQLException e) {
                return ConnectionTestResult.failure("Database connection failed: " + e.getMessage());
            }
        } catch (Exception e) {
            return ConnectionTestResult.failure("Unexpected error during connection test: " + e.getMessage());
        }
    }
}