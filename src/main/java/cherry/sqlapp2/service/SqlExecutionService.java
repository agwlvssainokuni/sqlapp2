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
import cherry.sqlapp2.entity.QueryHistory;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.DatabaseConnectionRepository;
import cherry.sqlapp2.util.SqlParameterExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.util.*;

@Service
public class SqlExecutionService {

    private final DynamicDataSourceService dataSourceService;
    private final QueryManagementService queryManagementService;
    private final DatabaseConnectionRepository connectionRepository;

    @Autowired
    public SqlExecutionService(
            DynamicDataSourceService dataSourceService,
            QueryManagementService queryManagementService,
            DatabaseConnectionRepository connectionRepository
    ) {
        this.dataSourceService = dataSourceService;
        this.queryManagementService = queryManagementService;
        this.connectionRepository = connectionRepository;
    }

    /**
     * Execute SQL query and return results with optional SavedQuery association
     */
    public SqlExecutionResult executeQuery(
            User user,
            Long connectionId,
            String sql,
            SavedQuery savedQuery
    ) {

        // Determine if this is a SELECT query or other operation
        String trimmedSql = sql.trim().toLowerCase();
        boolean isSelect = trimmedSql.startsWith("select") ||
                trimmedSql.startsWith("with") ||
                trimmedSql.startsWith("show") ||
                trimmedSql.startsWith("describe") ||
                trimmedSql.startsWith("desc") ||
                trimmedSql.startsWith("explain");

        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSourceService.getConnection(user, connectionId);
             Statement statement = connection.createStatement()) {

            SqlExecutionResult.SqlResultData data = null;
            if (isSelect) {
                // Execute SELECT query
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    data = processResultSet(resultSet);
                }
            } else {
                // Execute UPDATE/INSERT/DELETE/DDL
                @SuppressWarnings("unused")
                int affectedRows = statement.executeUpdate(sql);
            }
            long executionTime = System.currentTimeMillis() - startTime;
            LocalDateTime executedAt = LocalDateTime.now();

            // Record query execution in history
            cherry.sqlapp2.entity.QueryHistory queryHistory = recordQueryExecution(
                    user,
                    connectionId,
                    sql,
                    null,
                    executionTime,
                    Optional.ofNullable(data).map(SqlExecutionResult.SqlResultData::rowCount).orElse(null),
                    true,
                    null,
                    savedQuery
            );

            return SqlExecutionResult.success(
                    executedAt,
                    executionTime,
                    sql,
                    data,
                    Optional.ofNullable(queryHistory).map(QueryHistory::getId).orElse(null),
                    Optional.ofNullable(savedQuery).map(SavedQuery::getId).orElse(null)
            );

        } catch (SQLException ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            LocalDateTime executedAt = LocalDateTime.now();

            // Record failed query execution in history
            cherry.sqlapp2.entity.QueryHistory queryHistory = recordQueryExecution(
                    user,
                    connectionId,
                    sql,
                    null,
                    executionTime,
                    null,
                    false,
                    ex.getMessage(),
                    savedQuery
            );

            return SqlExecutionResult.error(
                    executedAt,
                    sql,
                    ex.getMessage(),
                    "SQLException",
                    ex.getErrorCode(),
                    ex.getSQLState(),
                    Optional.ofNullable(queryHistory).map(QueryHistory::getId).orElse(null),
                    Optional.ofNullable(savedQuery).map(SavedQuery::getId).orElse(null)
            );
        }
    }

    /**
     * Validate SQL query for basic security
     */
    public void validateQuery(String sql) throws IllegalArgumentException {

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
     * Execute parameterized SQL query and return results with optional SavedQuery association
     */
    public SqlExecutionResult executeParameterizedQuery(
            User user,
            Long connectionId,
            String sql,
            Map<String, Object> parameters,
            Map<String, String> parameterTypes,
            SavedQuery savedQuery
    ) {

        // Determine if this is a SELECT query or other operation
        String trimmedSql = sql.trim().toLowerCase();
        boolean isSelect = trimmedSql.startsWith("select") ||
                trimmedSql.startsWith("with") ||
                trimmedSql.startsWith("show") ||
                trimmedSql.startsWith("describe") ||
                trimmedSql.startsWith("desc") ||
                trimmedSql.startsWith("explain");

        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSourceService.getConnection(user, connectionId)) {

            // Convert named parameters to positioned parameters
            ParameterizedQuery paramQuery = convertNamedParameters(sql, parameters, parameterTypes);

            try (PreparedStatement statement = connection.prepareStatement(paramQuery.sql())) {

                // Set parameters
                setParameters(statement, paramQuery.parameters(), paramQuery.parameterTypes());

                SqlExecutionResult.SqlResultData data = null;
                if (isSelect) {
                    // Execute SELECT query
                    try (ResultSet resultSet = statement.executeQuery()) {
                        data = processResultSet(resultSet);
                    }
                } else {
                    // Execute UPDATE/INSERT/DELETE/DDL
                    @SuppressWarnings("unused")
                    int affectedRows = statement.executeUpdate();
                }
                long executionTime = System.currentTimeMillis() - startTime;
                LocalDateTime executedAt = LocalDateTime.now();

                // Record query execution in history
                cherry.sqlapp2.entity.QueryHistory queryHistory = recordQueryExecution(
                        user,
                        connectionId,
                        sql,
                        parameters,
                        executionTime,
                        Optional.ofNullable(data).map(SqlExecutionResult.SqlResultData::rowCount).orElse(null),
                        true,
                        null,
                        savedQuery
                );

                return SqlExecutionResult.success(
                        executedAt,
                        executionTime,
                        sql,
                        data,
                        Optional.ofNullable(queryHistory).map(QueryHistory::getId).orElse(null),
                        Optional.ofNullable(savedQuery).map(SavedQuery::getId).orElse(null)
                );
            }

        } catch (SQLException ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            LocalDateTime executedAt = LocalDateTime.now();

            // Record failed query execution in history
            cherry.sqlapp2.entity.QueryHistory queryHistory = recordQueryExecution(
                    user,
                    connectionId,
                    sql,
                    null,
                    executionTime,
                    null,
                    false,
                    ex.getMessage(),
                    savedQuery
            );

            return SqlExecutionResult.error(
                    executedAt,
                    sql,
                    ex.getMessage(),
                    "SQLException",
                    ex.getErrorCode(),
                    ex.getSQLState(),
                    Optional.ofNullable(queryHistory).map(QueryHistory::getId).orElse(null),
                    Optional.ofNullable(savedQuery).map(SavedQuery::getId).orElse(null)
            );
        }
    }

    /**
     * Process ResultSet and convert to structured data
     */
    private SqlExecutionResult.SqlResultData processResultSet(ResultSet resultSet) throws SQLException {

        // Get column metadata
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Get column names and detailed information for frontend
        List<String> columns = new ArrayList<>();
        List<SqlExecutionResult.ColumnDetail> columnDetails = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            columns.add(columnName);

            // Create column detail information
            SqlExecutionResult.ColumnDetail columnDetail = new SqlExecutionResult.ColumnDetail(
                    columnName,
                    metaData.getColumnLabel(i),
                    metaData.getColumnTypeName(i),
                    metaData.getColumnClassName(i),
                    metaData.isNullable(i) == ResultSetMetaData.columnNullable,
                    metaData.getPrecision(i),
                    metaData.getScale(i)
            );
            columnDetails.add(columnDetail);
        }

        // Process rows
        List<List<Object>> rows = new ArrayList<>();
        int rowCount = 0;

        while (resultSet.next() && rowCount < 1000) { // Limit to 1000 rows for now
            List<Object> row = new ArrayList<>(columnCount);

            for (int i = 1; i <= columnCount; i++) {
                Object value = resultSet.getObject(i);

                // Handle special cases for better JSON serialization
                switch (value) {
                    case Clob clob -> row.add(clob.getSubString(1, (int) clob.length()));
                    case Blob blob -> row.add("[BLOB data]");
                    case Date date -> row.add(date.toLocalDate());
                    case Time time -> row.add(
                            LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(time.getTime()),
                                    ZoneId.systemDefault()
                            ).toLocalTime()
                    );
                    case Timestamp timestamp -> row.add(timestamp.toLocalDateTime());
                    case null, default -> row.add(value);
                }
            }

            rows.add(row);
            rowCount++;
        }

        return new SqlExecutionResult.SqlResultData(
                columns,
                columnDetails,
                rows,
                rowCount
        );
    }

    /**
     * Convert named parameters (:param) to positioned parameters (?)
     * Uses sophisticated parsing to avoid extracting parameters from string literals and comments
     */
    private ParameterizedQuery convertNamedParameters(String sql, Map<String, Object> parameters,
                                                      Map<String, String> parameterTypes) {
        if (parameters == null || parameters.isEmpty()) {
            return new ParameterizedQuery(sql, new ArrayList<>(), new ArrayList<>());
        }

        // Extract parameters with positions using the sophisticated extractor
        List<SqlParameterExtractor.ParameterPosition> parameterPositions = 
                SqlParameterExtractor.extractParametersWithPositions(sql);
        
        List<Object> parameterValues = new ArrayList<>();
        List<String> parameterTypesList = new ArrayList<>();
        
        // Build the converted SQL by replacing parameters from right to left
        // to avoid position shifts during replacement
        StringBuilder convertedSqlBuilder = new StringBuilder(sql);
        
        // Process parameters in reverse order to maintain position accuracy
        for (int i = parameterPositions.size() - 1; i >= 0; i--) {
            SqlParameterExtractor.ParameterPosition paramPos = parameterPositions.get(i);
            String paramName = paramPos.name();
            
            if (parameters.containsKey(paramName)) {
                // Replace the specific parameter at its exact position
                convertedSqlBuilder.replace(paramPos.start(), paramPos.end(), "?");
            } else {
                throw new IllegalArgumentException("Parameter not provided: " + paramName);
            }
        }
        
        // Build parameter lists in original order (not reverse)
        Set<String> addedParams = new LinkedHashSet<>();
        for (SqlParameterExtractor.ParameterPosition paramPos : parameterPositions) {
            String paramName = paramPos.name();
            if (!addedParams.contains(paramName)) {
                addedParams.add(paramName);
                parameterValues.add(parameters.get(paramName));
                parameterTypesList.add(parameterTypes != null ? parameterTypes.get(paramName) : null);
            }
        }

        return new ParameterizedQuery(convertedSqlBuilder.toString(), parameterValues, parameterTypesList);
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
        switch (value) {
            case String s -> statement.setString(index, s);
            case Integer i -> statement.setInt(index, i);
            case Long l -> statement.setLong(index, l);
            case Double v -> statement.setDouble(index, v);
            case BigDecimal bigDecimal -> statement.setBigDecimal(index, bigDecimal);
            case Boolean b -> statement.setBoolean(index, b);
            case LocalDate localDate -> statement.setDate(index, Date.valueOf(localDate));
            case LocalTime localTime -> statement.setTime(index, Time.valueOf(localTime));
            case LocalDateTime localDateTime -> statement.setTimestamp(index, Timestamp.valueOf(localDateTime));
            default -> statement.setString(index, value.toString());
        }
    }

    /**
     * Internal class to hold parameterized query data
     */
    private record ParameterizedQuery(
            String sql,
            List<Object> parameters,
            List<String> parameterTypes
    ) {
    }

    // ==================== Query History Helper Methods ====================

    private QueryHistory recordQueryExecution(
            User user,
            Long connectionId,
            String sql,
            Map<String, Object> parameterValues,
            long executionTimeMs,
            Integer resultCount,
            boolean isSuccessful,
            String errorMessage,
            SavedQuery savedQuery
    ) {
        return connectionRepository.findByUserAndId(user, connectionId).map(connection ->
                queryManagementService.recordExecution(
                        sql,
                        parameterValues,
                        executionTimeMs,
                        resultCount,
                        isSuccessful,
                        errorMessage,
                        user,
                        connection,
                        savedQuery
                )
        ).orElse(null);
    }
}