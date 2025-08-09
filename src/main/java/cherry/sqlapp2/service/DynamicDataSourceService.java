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

import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicDataSourceService {

    private final DatabaseConnectionService connectionService;
    private final EncryptionService encryptionService;
    
    // Connection pool per user per database connection
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    
    @Autowired
    public DynamicDataSourceService(DatabaseConnectionService connectionService, 
                                   EncryptionService encryptionService) {
        this.connectionService = connectionService;
        this.encryptionService = encryptionService;
    }

    /**
     * Get a JDBC Connection for the specified user and connection ID
     */
    public Connection getConnection(User user, Long connectionId) throws SQLException {
        DatabaseConnection dbConfig = connectionService.getConnectionEntityById(user, connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
        
        if (!dbConfig.isActive()) {
            throw new IllegalStateException("Connection is not active: " + dbConfig.getConnectionName());
        }
        
        return createConnection(dbConfig);
    }
    
    /**
     * Get a JDBC Connection for the specified database connection entity
     */
    public Connection getConnection(DatabaseConnection dbConfig) throws SQLException {
        if (!dbConfig.isActive()) {
            throw new IllegalStateException("Connection is not active: " + dbConfig.getConnectionName());
        }
        
        return createConnection(dbConfig);
    }

    /**
     * Create a direct JDBC connection from DatabaseConnection entity
     */
    private Connection createConnection(DatabaseConnection dbConfig) throws SQLException {
        String decryptedPassword = encryptionService.decrypt(dbConfig.getEncryptedPassword());
        String connectionUrl = dbConfig.buildJdbcUrl();
        
        try {
            return java.sql.DriverManager.getConnection(
                    connectionUrl, 
                    dbConfig.getUsername(), 
                    decryptedPassword
            );
        } catch (SQLException e) {
            throw new SQLException(
                "Failed to connect to database '" + dbConfig.getConnectionName() + "': " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Test if connection is available and active
     */
    public boolean isConnectionAvailable(User user, Long connectionId) {
        try {
            DatabaseConnection dbConfig = connectionService.getConnectionEntityById(user, connectionId)
                    .orElse(null);
            
            if (dbConfig == null || !dbConfig.isActive()) {
                return false;
            }
            
            try (Connection conn = createConnection(dbConfig)) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get connection metadata for display purposes
     */
    public Map<String, Object> getConnectionInfo(User user, Long connectionId) throws SQLException {
        DatabaseConnection dbConfig = connectionService.getConnectionEntityById(user, connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
        
        Map<String, Object> info = new HashMap<>();
        
        try (Connection conn = createConnection(dbConfig)) {
            var metaData = conn.getMetaData();
            
            info.put("connectionName", dbConfig.getConnectionName());
            info.put("databaseType", dbConfig.getDatabaseType().toString());
            info.put("host", dbConfig.getHost());
            info.put("port", dbConfig.getPort());
            info.put("databaseName", dbConfig.getDatabaseName());
            info.put("username", dbConfig.getUsername());
            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("jdbcUrl", dbConfig.buildJdbcUrl());
            info.put("connected", conn.isValid(5));
            
        } catch (SQLException e) {
            info.put("connectionName", dbConfig.getConnectionName());
            info.put("databaseType", dbConfig.getDatabaseType().toString());
            info.put("host", dbConfig.getHost());
            info.put("port", dbConfig.getPort());
            info.put("databaseName", dbConfig.getDatabaseName());
            info.put("username", dbConfig.getUsername());
            info.put("connected", false);
            info.put("error", e.getMessage());
        }
        
        return info;
    }

    /**
     * Close and cleanup connections for a user
     */
    public void closeUserConnections(User user) {
        // In a more sophisticated implementation, we would maintain
        // connection pools per user and clean them up here
        // For now, we rely on the try-with-resources pattern
        // for individual connection management
    }
}