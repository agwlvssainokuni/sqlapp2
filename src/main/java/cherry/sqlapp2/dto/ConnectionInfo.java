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
package cherry.sqlapp2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectionInfo {
    
    @JsonProperty("connectionName")
    private String connectionName;
    
    @JsonProperty("databaseType")
    private String databaseType;
    
    @JsonProperty("host")
    private String host;
    
    @JsonProperty("port")
    private int port;
    
    @JsonProperty("databaseName")
    private String databaseName;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("databaseProductName")
    private String databaseProductName;
    
    @JsonProperty("databaseProductVersion")
    private String databaseProductVersion;
    
    @JsonProperty("driverName")
    private String driverName;
    
    @JsonProperty("driverVersion")
    private String driverVersion;
    
    @JsonProperty("jdbcUrl")
    private String jdbcUrl;
    
    @JsonProperty("connected")
    private boolean connected;
    
    @JsonProperty("error")
    private String error;

    // Default constructor
    public ConnectionInfo() {}

    // Getters and setters
    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDatabaseProductName() {
        return databaseProductName;
    }

    public void setDatabaseProductName(String databaseProductName) {
        this.databaseProductName = databaseProductName;
    }

    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }

    public void setDatabaseProductVersion(String databaseProductVersion) {
        this.databaseProductVersion = databaseProductVersion;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}