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

import java.util.List;
import java.util.Map;

public class QueryBuilderResponse {

    private String generatedSql;
    private boolean isValid;
    private List<String> validationErrors;
    private List<String> warnings;
    private Map<String, String> detectedParameters;
    private QueryStructure parsedStructure;
    private long buildTimeMs;

    // Constructors
    public QueryBuilderResponse() {}

    public QueryBuilderResponse(String generatedSql, boolean isValid) {
        this.generatedSql = generatedSql;
        this.isValid = isValid;
    }

    public static QueryBuilderResponse success(String generatedSql) {
        return new QueryBuilderResponse(generatedSql, true);
    }

    public static QueryBuilderResponse error(List<String> validationErrors) {
        QueryBuilderResponse response = new QueryBuilderResponse();
        response.setValid(false);
        response.setValidationErrors(validationErrors);
        return response;
    }

    // Getters and Setters
    public String getGeneratedSql() {
        return generatedSql;
    }

    public void setGeneratedSql(String generatedSql) {
        this.generatedSql = generatedSql;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public Map<String, String> getDetectedParameters() {
        return detectedParameters;
    }

    public void setDetectedParameters(Map<String, String> detectedParameters) {
        this.detectedParameters = detectedParameters;
    }

    public QueryStructure getParsedStructure() {
        return parsedStructure;
    }

    public void setParsedStructure(QueryStructure parsedStructure) {
        this.parsedStructure = parsedStructure;
    }

    public long getBuildTimeMs() {
        return buildTimeMs;
    }

    public void setBuildTimeMs(long buildTimeMs) {
        this.buildTimeMs = buildTimeMs;
    }
}