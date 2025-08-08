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
package cherry.sqlapp2.service;

import cherry.sqlapp2.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SqlExecutionService {

    private final DynamicDataSourceService dataSourceService;

    @Autowired
    public SqlExecutionService(DynamicDataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    /**
     * Execute SQL query and return results
     */
    public Map<String, Object> executeQuery(User user, Long connectionId, String sql) throws SQLException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }

        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSourceService.getConnection(user, connectionId);
             Statement statement = connection.createStatement()) {
            
            Map<String, Object> result = new LinkedHashMap<>();
            
            // Determine if this is a SELECT query or other operation
            String trimmedSql = sql.trim().toLowerCase();
            boolean isSelect = trimmedSql.startsWith("select") || 
                             trimmedSql.startsWith("with") || 
                             trimmedSql.startsWith("show") ||
                             trimmedSql.startsWith("describe") ||
                             trimmedSql.startsWith("desc") ||
                             trimmedSql.startsWith("explain");
            
            if (isSelect) {
                // Execute SELECT query
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    result = processResultSet(resultSet);
                }
            } else {
                // Execute UPDATE/INSERT/DELETE/DDL
                int affectedRows = statement.executeUpdate(sql);
                result.put("affectedRows", affectedRows);
                result.put("resultType", "update");
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            result.put("executionTimeMs", executionTime);
            result.put("executedAt", LocalDateTime.now());
            result.put("sql", sql);
            result.put("success", true);
            
            return result;
            
        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("sqlState", e.getSQLState());
            errorResult.put("errorCode", e.getErrorCode());
            errorResult.put("sql", sql);
            errorResult.put("executionTimeMs", executionTime);
            errorResult.put("executedAt", LocalDateTime.now());
            
            throw new SQLException("SQL execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process ResultSet and convert to structured data
     */
    private Map<String, Object> processResultSet(ResultSet resultSet) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // Get column metadata
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        List<Map<String, Object>> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("name", metaData.getColumnName(i));
            column.put("label", metaData.getColumnLabel(i));
            column.put("type", metaData.getColumnTypeName(i));
            column.put("className", metaData.getColumnClassName(i));
            column.put("nullable", metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            column.put("precision", metaData.getPrecision(i));
            column.put("scale", metaData.getScale(i));
            columns.add(column);
        }
        
        // Process rows
        List<Map<String, Object>> rows = new ArrayList<>();
        int rowCount = 0;
        
        while (resultSet.next() && rowCount < 1000) { // Limit to 1000 rows for now
            Map<String, Object> row = new LinkedHashMap<>();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                
                // Handle special cases for better JSON serialization
                if (value instanceof Clob) {
                    Clob clob = (Clob) value;
                    value = clob.getSubString(1, (int) clob.length());
                } else if (value instanceof Blob) {
                    value = "[BLOB data]";
                } else if (value instanceof Date) {
                    value = value.toString();
                } else if (value instanceof Time) {
                    value = value.toString();
                } else if (value instanceof Timestamp) {
                    value = value.toString();
                }
                
                row.put(columnName, value);
            }
            
            rows.add(row);
            rowCount++;
        }
        
        result.put("resultType", "select");
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("rowCount", rowCount);
        result.put("columnCount", columnCount);
        
        // Check if there are more rows
        if (resultSet.next()) {
            result.put("hasMoreRows", true);
            result.put("note", "Result limited to 1000 rows");
        } else {
            result.put("hasMoreRows", false);
        }
        
        return result;
    }

    /**
     * Validate SQL query for basic security
     */
    public void validateQuery(String sql) throws IllegalArgumentException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }

        String trimmedSql = sql.trim().toLowerCase();
        
        // Basic validation - prevent potentially dangerous operations
        List<String> dangerousPatterns = List.of(
            "drop database",
            "drop schema", 
            "drop table",
            "drop view",
            "drop index",
            "truncate",
            "alter table",
            "alter database",
            "alter schema",
            "create user",
            "drop user",
            "grant",
            "revoke"
        );
        
        for (String pattern : dangerousPatterns) {
            if (trimmedSql.contains(pattern)) {
                throw new IllegalArgumentException("Potentially dangerous SQL operation detected: " + pattern);
            }
        }
        
        // Check for maximum length
        if (sql.length() > 10000) {
            throw new IllegalArgumentException("SQL query too long (maximum 10,000 characters)");
        }
    }
}