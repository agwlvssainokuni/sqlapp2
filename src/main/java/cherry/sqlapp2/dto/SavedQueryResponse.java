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

import cherry.sqlapp2.entity.SavedQuery;

import java.time.LocalDateTime;
import java.util.Map;

public class SavedQueryResponse {

    private Long id;
    private String name;
    private String sqlContent;
    private String description;
    private Map<String, String> parameterDefinitions;
    private SavedQuery.SharingScope sharingScope;
    private String username;
    private Long userId;
    private DatabaseConnectionResponse defaultConnection;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastExecutedAt;
    private Integer executionCount;

    // Constructors
    public SavedQueryResponse() {}

    public SavedQueryResponse(SavedQuery savedQuery) {
        this.id = savedQuery.getId();
        this.name = savedQuery.getName();
        this.sqlContent = savedQuery.getSqlContent();
        this.description = savedQuery.getDescription();
        this.sharingScope = savedQuery.getSharingScope();
        this.username = savedQuery.getUser().getUsername();
        this.userId = savedQuery.getUser().getId();
        this.createdAt = savedQuery.getCreatedAt();
        this.updatedAt = savedQuery.getUpdatedAt();
        this.lastExecutedAt = savedQuery.getLastExecutedAt();
        this.executionCount = savedQuery.getExecutionCount();

        if (savedQuery.getDefaultConnection() != null) {
            this.defaultConnection = new DatabaseConnectionResponse(savedQuery.getDefaultConnection());
        }
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getParameterDefinitions() {
        return parameterDefinitions;
    }

    public void setParameterDefinitions(Map<String, String> parameterDefinitions) {
        this.parameterDefinitions = parameterDefinitions;
    }

    public SavedQuery.SharingScope getSharingScope() {
        return sharingScope;
    }

    public void setSharingScope(SavedQuery.SharingScope sharingScope) {
        this.sharingScope = sharingScope;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DatabaseConnectionResponse getDefaultConnection() {
        return defaultConnection;
    }

    public void setDefaultConnection(DatabaseConnectionResponse defaultConnection) {
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
}