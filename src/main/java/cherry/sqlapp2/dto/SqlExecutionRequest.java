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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class SqlExecutionRequest {

    @JsonProperty("connectionId")
    @NotNull(message = "Connection ID is required")
    private Long connectionId;

    @JsonProperty("sql")
    @NotBlank(message = "SQL query is required")
    @Size(max = 10000, message = "SQL query must be less than 10,000 characters")
    private String sql;

    @JsonProperty("validateOnly")
    private boolean validateOnly = false;

    @JsonProperty("maxRows")
    private Integer maxRows;

    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    @JsonProperty("parameterTypes")
    private Map<String, String> parameterTypes;

    @JsonProperty("savedQueryId")
    private Long savedQueryId;

    @JsonProperty("pagingRequest")
    private PagingRequest pagingRequest;

    // Default constructor
    public SqlExecutionRequest() {}

    public SqlExecutionRequest(Long connectionId, String sql) {
        this.connectionId = connectionId;
        this.sql = sql;
    }

    // Getters and setters
    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public boolean isValidateOnly() {
        return validateOnly;
    }

    public void setValidateOnly(boolean validateOnly) {
        this.validateOnly = validateOnly;
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(Integer maxRows) {
        this.maxRows = maxRows;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Map<String, String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Long getSavedQueryId() {
        return savedQueryId;
    }

    public void setSavedQueryId(Long savedQueryId) {
        this.savedQueryId = savedQueryId;
    }

    public PagingRequest getPagingRequest() {
        return pagingRequest;
    }

    public void setPagingRequest(PagingRequest pagingRequest) {
        this.pagingRequest = pagingRequest;
    }
}