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

package cherry.sqlapp2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for database connection information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConnectionInfoResponse(
    String connectionName,
    String databaseType,
    String host,
    Integer port,
    String databaseName,
    String username,
    String databaseProductName,
    String databaseProductVersion,
    String driverName,
    String driverVersion,
    String jdbcUrl,
    Boolean connected,
    String error
) {
    
    /**
     * Create a successful connection info result
     */
    public static ConnectionInfoResponse success(
            String connectionName,
            String databaseType,
            String host,
            Integer port,
            String databaseName,
            String username,
            String databaseProductName,
            String databaseProductVersion,
            String driverName,
            String driverVersion,
            String jdbcUrl,
            Boolean connected) {
        return new ConnectionInfoResponse(
                connectionName,
                databaseType,
                host,
                port,
                databaseName,
                username,
                databaseProductName,
                databaseProductVersion,
                driverName,
                driverVersion,
                jdbcUrl,
                connected,
                null
        );
    }
    
    /**
     * Create a connection info result with error
     */
    public static ConnectionInfoResponse error(
            String connectionName,
            String databaseType,
            String host,
            Integer port,
            String databaseName,
            String username,
            String error) {
        return new ConnectionInfoResponse(
                connectionName,
                databaseType,
                host,
                port,
                databaseName,
                username,
                null,
                null,
                null,
                null,
                null,
                false,
                error
        );
    }
}