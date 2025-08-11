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

package cherry.sqlapp2.controller;

import cherry.sqlapp2.dto.ApiResponse;
import cherry.sqlapp2.dto.QueryBuilderRequest;
import cherry.sqlapp2.dto.QueryBuilderResponse;
import cherry.sqlapp2.service.QueryBuilderService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query-builder")
public class QueryBuilderController {

    @Autowired
    private QueryBuilderService queryBuilderService;

    /**
     * Build SQL query from structure
     */
    @PostMapping("/build")
    public ApiResponse<QueryBuilderResponse> buildQuery(
            @Valid @RequestBody QueryBuilderRequest request,
            Authentication authentication) {
        
        try {
            QueryBuilderResponse response = queryBuilderService.buildQuery(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(List.of("Failed to build query: " + e.getMessage()));
        }
    }

    /**
     * Validate query structure without building SQL
     */
    @Deprecated
    @PostMapping("/validate")
    public ApiResponse<QueryBuilderResponse> validateQuery(
            @Valid @RequestBody QueryBuilderRequest request,
            Authentication authentication) {
        
        try {
            // Set validation only mode
            request.setValidateSyntax(true);
            request.setFormatSql(false);
            
            QueryBuilderResponse response = queryBuilderService.buildQuery(request);
            
            // Clear generated SQL for validation-only response
            if (response.isValid()) {
                QueryBuilderResponse validationResponse = new QueryBuilderResponse();
                validationResponse.setValid(true);
                validationResponse.setBuildTimeMs(response.getBuildTimeMs());
                return ApiResponse.success(validationResponse);
            } else {
                return ApiResponse.error(response.getValidationErrors());
            }
        } catch (Exception e) {
            return ApiResponse.error(List.of("Validation failed: " + e.getMessage()));
        }
    }

    /**
     * Get query building suggestions based on partial structure
     */
    @Deprecated
    @PostMapping("/suggestions")
    public ApiResponse<QueryBuilderSuggestions> getQuerySuggestions(
            @RequestBody QuerySuggestionsRequest request,
            Authentication authentication) {
        
        try {
            // This would integrate with schema service to provide intelligent suggestions
            // For now, return a placeholder response
            QueryBuilderSuggestions suggestions = new QueryBuilderSuggestions();
            
            return ApiResponse.success(suggestions);
        } catch (Exception e) {
            return ApiResponse.error(List.of("Failed to get query suggestions: " + e.getMessage()));
        }
    }

    // Supporting classes for suggestions endpoint
    public static class QuerySuggestionsRequest {
        private String currentTable;
        private java.util.List<String> selectedTables;
        private String suggestionType; // "columns", "tables", "joins", "conditions"

        // Getters and setters
        public String getCurrentTable() { return currentTable; }
        public void setCurrentTable(String currentTable) { this.currentTable = currentTable; }
        
        public java.util.List<String> getSelectedTables() { return selectedTables; }
        public void setSelectedTables(java.util.List<String> selectedTables) { this.selectedTables = selectedTables; }
        
        public String getSuggestionType() { return suggestionType; }
        public void setSuggestionType(String suggestionType) { this.suggestionType = suggestionType; }
    }

    public static class QueryBuilderSuggestions {
        private java.util.List<ColumnSuggestion> columns = new java.util.ArrayList<>();
        private java.util.List<TableSuggestion> tables = new java.util.ArrayList<>();
        private java.util.List<JoinSuggestion> joins = new java.util.ArrayList<>();

        public static class ColumnSuggestion {
            private String tableName;
            private String columnName;
            private String dataType;
            private boolean isPrimaryKey;
            private boolean isForeignKey;
            private String description;

            // Constructors
            public ColumnSuggestion() {}

            public ColumnSuggestion(String tableName, String columnName, String dataType) {
                this.tableName = tableName;
                this.columnName = columnName;
                this.dataType = dataType;
            }

            // Getters and setters
            public String getTableName() { return tableName; }
            public void setTableName(String tableName) { this.tableName = tableName; }
            
            public String getColumnName() { return columnName; }
            public void setColumnName(String columnName) { this.columnName = columnName; }
            
            public String getDataType() { return dataType; }
            public void setDataType(String dataType) { this.dataType = dataType; }
            
            public boolean isPrimaryKey() { return isPrimaryKey; }
            public void setPrimaryKey(boolean primaryKey) { isPrimaryKey = primaryKey; }
            
            public boolean isForeignKey() { return isForeignKey; }
            public void setForeignKey(boolean foreignKey) { isForeignKey = foreignKey; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
        }

        public static class TableSuggestion {
            private String tableName;
            private String description;
            private int columnCount;

            public TableSuggestion() {}

            public TableSuggestion(String tableName, String description, int columnCount) {
                this.tableName = tableName;
                this.description = description;
                this.columnCount = columnCount;
            }

            // Getters and setters
            public String getTableName() { return tableName; }
            public void setTableName(String tableName) { this.tableName = tableName; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            
            public int getColumnCount() { return columnCount; }
            public void setColumnCount(int columnCount) { this.columnCount = columnCount; }
        }

        public static class JoinSuggestion {
            private String sourceTable;
            private String targetTable;
            private String sourceColumn;
            private String targetColumn;
            private String suggestedJoinType;
            private double confidence;

            public JoinSuggestion() {}

            public JoinSuggestion(String sourceTable, String targetTable, 
                                String sourceColumn, String targetColumn, String suggestedJoinType) {
                this.sourceTable = sourceTable;
                this.targetTable = targetTable;
                this.sourceColumn = sourceColumn;
                this.targetColumn = targetColumn;
                this.suggestedJoinType = suggestedJoinType;
            }

            // Getters and setters
            public String getSourceTable() { return sourceTable; }
            public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
            
            public String getTargetTable() { return targetTable; }
            public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
            
            public String getSourceColumn() { return sourceColumn; }
            public void setSourceColumn(String sourceColumn) { this.sourceColumn = sourceColumn; }
            
            public String getTargetColumn() { return targetColumn; }
            public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }
            
            public String getSuggestedJoinType() { return suggestedJoinType; }
            public void setSuggestedJoinType(String suggestedJoinType) { this.suggestedJoinType = suggestedJoinType; }
            
            public double getConfidence() { return confidence; }
            public void setConfidence(double confidence) { this.confidence = confidence; }
        }

        // Main class getters and setters
        public java.util.List<ColumnSuggestion> getColumns() { return columns; }
        public void setColumns(java.util.List<ColumnSuggestion> columns) { this.columns = columns; }
        
        public java.util.List<TableSuggestion> getTables() { return tables; }
        public void setTables(java.util.List<TableSuggestion> tables) { this.tables = tables; }
        
        public java.util.List<JoinSuggestion> getJoins() { return joins; }
        public void setJoins(java.util.List<JoinSuggestion> joins) { this.joins = joins; }
    }
}