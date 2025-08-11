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

import cherry.sqlapp2.dto.SqlExecutionResult;
import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.DatabaseConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SqlExecutionService {

    private final DynamicDataSourceService dataSourceService;
    private final QueryManagementService queryManagementService;
    private final DatabaseConnectionRepository connectionRepository;

    @Autowired
    public SqlExecutionService(DynamicDataSourceService dataSourceService, 
                             QueryManagementService queryManagementService,
                             DatabaseConnectionRepository connectionRepository) {
        this.dataSourceService = dataSourceService;
        this.queryManagementService = queryManagementService;
        this.connectionRepository = connectionRepository;
    }

    /**
     * Execute SQL query and return results
     */
    public SqlExecutionResult executeQuery(User user, Long connectionId, String sql) throws SQLException {
        return executeQuery(user, connectionId, sql, null);
    }

    /**
     * Execute SQL query and return results with optional SavedQuery association
     */
    public SqlExecutionResult executeQuery(User user, Long connectionId, String sql, SavedQuery savedQuery) throws SQLException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }

        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSourceService.getConnection(user, connectionId);
             Statement statement = connection.createStatement()) {
            
            LocalDateTime executedAt = LocalDateTime.now();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Determine if this is a SELECT query or other operation
            String trimmedSql = sql.trim().toLowerCase();
            boolean isSelect = trimmedSql.startsWith("select") || 
                             trimmedSql.startsWith("with") || 
                             trimmedSql.startsWith("show") ||
                             trimmedSql.startsWith("describe") ||
                             trimmedSql.startsWith("desc") ||
                             trimmedSql.startsWith("explain");
            
            SqlExecutionResult result;
            if (isSelect) {
                // Execute SELECT query
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    var processedResult = processResultSet(resultSet);
                    result = SqlExecutionResult.selectResult(
                            (List<String>) processedResult.get("columns"),
                            (List<Map<String, Object>>) processedResult.get("rows"),
                            (Integer) processedResult.get("rowCount"),
                            (Integer) processedResult.get("columnCount"),
                            (Boolean) processedResult.get("hasMoreRows"),
                            (String) processedResult.get("note"),
                            executionTime,
                            executedAt,
                            sql
                    );
                }
            } else {
                // Execute UPDATE/INSERT/DELETE/DDL
                int affectedRows = statement.executeUpdate(sql);
                result = SqlExecutionResult.updateResult(
                        affectedRows,
                        executionTime,
                        executedAt,
                        sql
                );
            }
            
            // Record query execution in history
            recordQueryExecution(user, connectionId, sql, null, executionTime, 
                               getResultCount(result), true, null, savedQuery);
            
            return result;
            
        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LocalDateTime executedAt = LocalDateTime.now();
            
            // Record failed query execution in history
            recordQueryExecution(user, connectionId, sql, null, executionTime, 
                               null, false, e.getMessage(), savedQuery);
            
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
        
        // Get column names for frontend (string array)
        // TODO: 将来的にはカラムの詳細情報（type, nullable, precision等）も返却する
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> columnDetails = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnName(i));
            
            // 詳細情報は将来の拡張用に保持
            Map<String, Object> columnDetail = new LinkedHashMap<>();
            columnDetail.put("name", metaData.getColumnName(i));
            columnDetail.put("label", metaData.getColumnLabel(i));
            columnDetail.put("type", metaData.getColumnTypeName(i));
            columnDetail.put("className", metaData.getColumnClassName(i));
            columnDetail.put("nullable", metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            columnDetail.put("precision", metaData.getPrecision(i));
            columnDetail.put("scale", metaData.getScale(i));
            columnDetails.add(columnDetail);
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

    /**
     * Execute parameterized SQL query and return results
     */
    public SqlExecutionResult executeParameterizedQuery(User user, Long connectionId, String sql, 
                                                        Map<String, Object> parameters, 
                                                        Map<String, String> parameterTypes) throws SQLException {
        return executeParameterizedQuery(user, connectionId, sql, parameters, parameterTypes, null);
    }

    /**
     * Execute parameterized SQL query and return results with optional SavedQuery association
     */
    public SqlExecutionResult executeParameterizedQuery(User user, Long connectionId, String sql, 
                                                        Map<String, Object> parameters, 
                                                        Map<String, String> parameterTypes, 
                                                        SavedQuery savedQuery) throws SQLException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }

        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSourceService.getConnection(user, connectionId)) {
            
            LocalDateTime executedAt = LocalDateTime.now();
            
            // Convert named parameters to positioned parameters
            ParameterizedQuery paramQuery = convertNamedParameters(sql, parameters, parameterTypes);
            
            try (PreparedStatement statement = connection.prepareStatement(paramQuery.getSql())) {
                
                // Set parameters
                setParameters(statement, paramQuery.getParameters(), paramQuery.getParameterTypes());
                
                long executionTime = System.currentTimeMillis() - startTime;
                
                // Determine if this is a SELECT query or other operation
                String trimmedSql = sql.trim().toLowerCase();
                boolean isSelect = trimmedSql.startsWith("select") || 
                                 trimmedSql.startsWith("with") || 
                                 trimmedSql.startsWith("show") ||
                                 trimmedSql.startsWith("describe") ||
                                 trimmedSql.startsWith("desc") ||
                                 trimmedSql.startsWith("explain");
                
                SqlExecutionResult result;
                if (isSelect) {
                    // Execute SELECT query
                    try (ResultSet resultSet = statement.executeQuery()) {
                        var processedResult = processResultSet(resultSet);
                        result = SqlExecutionResult.selectResult(
                                (List<String>) processedResult.get("columns"),
                                (List<Map<String, Object>>) processedResult.get("rows"),
                                (Integer) processedResult.get("rowCount"),
                                (Integer) processedResult.get("columnCount"),
                                (Boolean) processedResult.get("hasMoreRows"),
                                (String) processedResult.get("note"),
                                executionTime,
                                executedAt,
                                sql
                        );
                    }
                } else {
                    // Execute UPDATE/INSERT/DELETE/DDL
                    int affectedRows = statement.executeUpdate();
                    result = SqlExecutionResult.updateResult(
                            affectedRows,
                            executionTime,
                            executedAt,
                            sql
                    );
                }
                
                // Record query execution in history
                recordQueryExecution(user, connectionId, sql, parameters, executionTime, 
                                   getResultCount(result), true, null, savedQuery);
                
                return result;
            }
            
        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LocalDateTime executedAt = LocalDateTime.now();
            
            // Record failed query execution in history
            recordQueryExecution(user, connectionId, sql, parameters, executionTime, 
                               null, false, e.getMessage(), savedQuery);
            
            throw new SQLException("Parameterized SQL execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert named parameters (:param) to positioned parameters (?)
     */
    private ParameterizedQuery convertNamedParameters(String sql, Map<String, Object> parameters, 
                                                    Map<String, String> parameterTypes) {
        if (parameters == null || parameters.isEmpty()) {
            return new ParameterizedQuery(sql, new ArrayList<>(), new ArrayList<>());
        }

        // Pattern to match named parameters like :paramName
        Pattern pattern = Pattern.compile(":(\\w+)");
        Matcher matcher = pattern.matcher(sql);
        
        List<Object> parameterValues = new ArrayList<>();
        List<String> parameterTypesList = new ArrayList<>();
        String convertedSql = sql;
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (parameters.containsKey(paramName)) {
                parameterValues.add(parameters.get(paramName));
                parameterTypesList.add(parameterTypes != null ? parameterTypes.get(paramName) : null);
                convertedSql = convertedSql.replaceFirst(":" + paramName, "?");
            } else {
                throw new IllegalArgumentException("Parameter not provided: " + paramName);
            }
        }
        
        return new ParameterizedQuery(convertedSql, parameterValues, parameterTypesList);
    }

    /**
     * Set parameters in PreparedStatement
     */
    private void setParameters(PreparedStatement statement, List<Object> parameters, 
                             List<String> parameterTypes) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            String type = i < parameterTypes.size() ? parameterTypes.get(i) : null;
            
            if (value == null) {
                statement.setNull(i + 1, Types.NULL);
            } else if (type != null) {
                setTypedParameter(statement, i + 1, value, type);
            } else {
                setAutoTypedParameter(statement, i + 1, value);
            }
        }
    }

    /**
     * Set parameter with explicit type
     */
    private void setTypedParameter(PreparedStatement statement, int index, Object value, String type) throws SQLException {
        switch (type.toLowerCase()) {
            case "string":
            case "varchar":
                statement.setString(index, value.toString());
                break;
            case "int":
            case "integer":
                statement.setInt(index, ((Number) value).intValue());
                break;
            case "long":
            case "bigint":
                statement.setLong(index, ((Number) value).longValue());
                break;
            case "double":
                statement.setDouble(index, ((Number) value).doubleValue());
                break;
            case "decimal":
            case "numeric":
                statement.setBigDecimal(index, new BigDecimal(value.toString()));
                break;
            case "boolean":
                statement.setBoolean(index, (Boolean) value);
                break;
            case "date":
                if (value instanceof LocalDate) {
                    statement.setDate(index, Date.valueOf((LocalDate) value));
                } else {
                    statement.setDate(index, Date.valueOf(LocalDate.parse(value.toString())));
                }
                break;
            case "time":
                if (value instanceof LocalTime) {
                    statement.setTime(index, Time.valueOf((LocalTime) value));
                } else {
                    statement.setTime(index, Time.valueOf(LocalTime.parse(value.toString())));
                }
                break;
            case "datetime":
            case "timestamp":
                if (value instanceof LocalDateTime) {
                    statement.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
                } else {
                    statement.setTimestamp(index, Timestamp.valueOf(LocalDateTime.parse(value.toString())));
                }
                break;
            default:
                setAutoTypedParameter(statement, index, value);
        }
    }

    /**
     * Set parameter with automatic type detection
     */
    private void setAutoTypedParameter(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value instanceof String) {
            statement.setString(index, (String) value);
        } else if (value instanceof Integer) {
            statement.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            statement.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            statement.setDouble(index, (Double) value);
        } else if (value instanceof BigDecimal) {
            statement.setBigDecimal(index, (BigDecimal) value);
        } else if (value instanceof Boolean) {
            statement.setBoolean(index, (Boolean) value);
        } else if (value instanceof LocalDate) {
            statement.setDate(index, Date.valueOf((LocalDate) value));
        } else if (value instanceof LocalTime) {
            statement.setTime(index, Time.valueOf((LocalTime) value));
        } else if (value instanceof LocalDateTime) {
            statement.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
        } else {
            statement.setString(index, value.toString());
        }
    }

    /**
     * Internal class to hold parameterized query data
     */
    private static class ParameterizedQuery {
        private final String sql;
        private final List<Object> parameters;
        private final List<String> parameterTypes;

        public ParameterizedQuery(String sql, List<Object> parameters, List<String> parameterTypes) {
            this.sql = sql;
            this.parameters = parameters;
            this.parameterTypes = parameterTypes;
        }

        public String getSql() { return sql; }
        public List<Object> getParameters() { return parameters; }
        public List<String> getParameterTypes() { return parameterTypes; }
    }

    // ==================== Query History Helper Methods ====================

    private void recordQueryExecution(User user, Long connectionId, String sql, 
                                    Map<String, Object> parameterValues, long executionTimeMs,
                                    Integer resultCount, boolean isSuccessful, String errorMessage,
                                    SavedQuery savedQuery) {
        try {
            DatabaseConnection connection = connectionRepository.findByUserAndId(user, connectionId)
                .orElse(null);
            
            if (connection != null) {
                queryManagementService.recordExecution(sql, parameterValues, executionTimeMs, 
                                                     resultCount, isSuccessful, errorMessage, 
                                                     user, connection, savedQuery);
            }
        } catch (Exception e) {
            // Don't fail the main query execution if history recording fails
            System.err.println("Failed to record query execution in history: " + e.getMessage());
        }
    }

    private Integer getResultCount(SqlExecutionResult result) {
        if (result.rowCount() != null) {
            return result.rowCount();
        }
        if (result.affectedRows() != null) {
            return result.affectedRows();
        }
        return null;
    }
}