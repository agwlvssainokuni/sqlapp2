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

package cherry.sqlapp2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_queries")
public class SavedQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "sql_content", nullable = false, columnDefinition = "TEXT")
    private String sqlContent;

    @Column(name = "parameter_definitions", columnDefinition = "TEXT")
    private String parameterDefinitions;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_scope", nullable = false)
    private SharingScope sharingScope = SharingScope.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id")
    private DatabaseConnection defaultConnection;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "execution_count", nullable = false)
    private Integer executionCount = 0;

    public enum SharingScope {
        PRIVATE, PUBLIC
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

    // Constructors
    public SavedQuery() {}

    public SavedQuery(String name, String sqlContent, String description, User user) {
        this.name = name;
        this.sqlContent = sqlContent;
        this.description = description;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSqlContent() {
        return sqlContent;
    }

    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }

    public String getParameterDefinitions() {
        return parameterDefinitions;
    }

    public void setParameterDefinitions(String parameterDefinitions) {
        this.parameterDefinitions = parameterDefinitions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SharingScope getSharingScope() {
        return sharingScope;
    }

    public void setSharingScope(SharingScope sharingScope) {
        this.sharingScope = sharingScope;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DatabaseConnection getDefaultConnection() {
        return defaultConnection;
    }

    public void setDefaultConnection(DatabaseConnection defaultConnection) {
        this.defaultConnection = defaultConnection;
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

    public LocalDateTime getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(LocalDateTime lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public Integer getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Integer executionCount) {
        this.executionCount = executionCount;
    }

    public void incrementExecutionCount() {
        this.executionCount = (this.executionCount == null ? 0 : this.executionCount) + 1;
        this.lastExecutedAt = LocalDateTime.now();
    }
}