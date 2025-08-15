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
package cherry.sqlapp2.entity;

import cherry.sqlapp2.enums.DatabaseType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * データベース接続情報を表すエンティティクラス。
 * ユーザが登録した外部データベースへの接続設定を管理します。
 * パスワードはAES-256-GCMで暗号化されて保存されます。
 */
@Entity
@Table(name = "database_connections")
public class DatabaseConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "connection_name", nullable = false, length = 100)
    @NotBlank(message = "Connection name is required")
    @Size(min = 1, max = 100, message = "Connection name must be between 1 and 100 characters")
    private String connectionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "database_type", nullable = false, length = 20)
    @NotNull(message = "Database type is required")
    private DatabaseType databaseType;

    @Column(name = "host", nullable = false, length = 255)
    @NotBlank(message = "Host is required")
    @Size(min = 1, max = 255, message = "Host must be between 1 and 255 characters")
    private String host;

    @Column(name = "port", nullable = false)
    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than or equal to 65535")
    private int port;

    @Column(name = "database_name", nullable = false, length = 100)
    @NotBlank(message = "Database name is required")
    @Size(min = 1, max = 100, message = "Database name must be between 1 and 100 characters")
    private String databaseName;

    @Column(name = "username", nullable = false, length = 100)
    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 100, message = "Username must be between 1 and 100 characters")
    private String username;

    @Column(name = "encrypted_password", nullable = false, length = 500)
    @NotBlank(message = "Password is required")
    private String encryptedPassword;

    @Column(name = "additional_params", length = 1000)
    private String additionalParams;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DatabaseConnection() {}

    public DatabaseConnection(User user, String connectionName, DatabaseType databaseType, 
                            String host, int port, String databaseName, String username) {
        this.user = user;
        this.connectionName = connectionName;
        this.databaseType = databaseType;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(DatabaseType databaseType) {
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

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(String additionalParams) {
        this.additionalParams = additionalParams;
    }

    public String buildJdbcUrl() {
        String baseUrl = databaseType.buildUrl(host, port, databaseName);
        if (additionalParams != null && !additionalParams.trim().isEmpty()) {
            // additionalParamsが?から始まっていない場合は?を追加
            String params = additionalParams.trim();
            if (!params.startsWith("?")) {
                params = "?" + params;
            }
            return baseUrl + params;
        }
        return baseUrl;
    }

    @Override
    public String toString() {
        return "DatabaseConnection{" +
                "id=" + id +
                ", connectionName='" + connectionName + '\'' +
                ", databaseType=" + databaseType +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", databaseName='" + databaseName + '\'' +
                ", username='" + username + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}