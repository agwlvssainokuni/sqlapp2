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

import cherry.sqlapp2.entity.QueryHistory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

public class QueryHistoryResponse {

    private Long id;
    private String sqlContent;
    private Map<String, Object> parameterValues;
    private Long executionTimeMs;
    private Integer resultCount;
    private Boolean isSuccessful;
    private String errorMessage;
    private String connectionName;
    private String databaseType;
    private Long connectionId;
    private Long savedQueryId;
    private String savedQueryName;
    private LocalDateTime executedAt;

    // Constructors
    public QueryHistoryResponse() {}

    public QueryHistoryResponse(QueryHistory queryHistory) {
        this.id = queryHistory.getId();
        this.sqlContent = queryHistory.getSqlContent();
        this.executionTimeMs = queryHistory.getExecutionTimeMs();
        this.resultCount = queryHistory.getResultCount();
        this.isSuccessful = queryHistory.getIsSuccessful();
        this.errorMessage = queryHistory.getErrorMessage();
        this.connectionName = queryHistory.getConnectionName();
        this.databaseType = queryHistory.getDatabaseType();
        this.executedAt = queryHistory.getExecutedAt();

        // Parse parameter values from JSON string
        if (queryHistory.getParameterValues() != null && !queryHistory.getParameterValues().trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.parameterValues = mapper.readValue(queryHistory.getParameterValues(), 
                    new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                // If JSON parsing fails, leave parameterValues as null
                this.parameterValues = null;
            }
        }

        if (queryHistory.getConnection() != null) {
            this.connectionId = queryHistory.getConnection().getId();
        }

        if (queryHistory.getSavedQuery() != null) {
            this.savedQueryId = queryHistory.getSavedQuery().getId();
            this.savedQueryName = queryHistory.getSavedQuery().getName();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSqlContent() {
        return sqlContent;
    }

    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }

    public Map<String, Object> getParameterValues() {
        return parameterValues;
    }

    public void setParameterValues(Map<String, Object> parameterValues) {
        this.parameterValues = parameterValues;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public Boolean getIsSuccessful() {
        return isSuccessful;
    }

    public void setIsSuccessful(Boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

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

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public Long getSavedQueryId() {
        return savedQueryId;
    }

    public void setSavedQueryId(Long savedQueryId) {
        this.savedQueryId = savedQueryId;
    }

    public String getSavedQueryName() {
        return savedQueryName;
    }

    public void setSavedQueryName(String savedQueryName) {
        this.savedQueryName = savedQueryName;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}