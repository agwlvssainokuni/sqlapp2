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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class SavedQueryRequest {

    @NotBlank(message = "Query name is required")
    @Size(max = 200, message = "Query name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "SQL content is required")
    private String sqlContent;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description = "";

    private Map<String, String> parameterDefinitions;

    private SavedQuery.SharingScope sharingScope = SavedQuery.SharingScope.PRIVATE;

    private Long defaultConnectionId;

    // Constructors
    public SavedQueryRequest() {}

    public SavedQueryRequest(String name, String sqlContent, String description) {
        this.name = name;
        this.sqlContent = sqlContent;
        this.description = description;
    }

    // Getters and Setters
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

    public Long getDefaultConnectionId() {
        return defaultConnectionId;
    }

    public void setDefaultConnectionId(Long defaultConnectionId) {
        this.defaultConnectionId = defaultConnectionId;
    }
}