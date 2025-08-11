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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for SQL execution results
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SqlExecutionResult(
    boolean success,
    String resultType,
    List<String> columns,
    List<Map<String, Object>> rows,
    Integer rowCount,
    Integer columnCount,
    Integer affectedRows,
    Boolean hasMoreRows,
    String note,
    Long executionTimeMs,
    LocalDateTime executedAt,
    String sql,
    String error,
    String sqlState,
    Integer errorCode
) {
    
    /**
     * Create a successful SELECT result
     */
    public static SqlExecutionResult selectResult(
            List<String> columns,
            List<Map<String, Object>> rows,
            Integer rowCount,
            Integer columnCount,
            Boolean hasMoreRows,
            String note,
            Long executionTimeMs,
            LocalDateTime executedAt,
            String sql) {
        return new SqlExecutionResult(
                true,
                "select",
                columns,
                rows,
                rowCount,
                columnCount,
                null,
                hasMoreRows,
                note,
                executionTimeMs,
                executedAt,
                sql,
                null,
                null,
                null
        );
    }
    
    /**
     * Create a successful UPDATE/INSERT/DELETE result
     */
    public static SqlExecutionResult updateResult(
            Integer affectedRows,
            Long executionTimeMs,
            LocalDateTime executedAt,
            String sql) {
        return new SqlExecutionResult(
                true,
                "update",
                null,
                null,
                null,
                null,
                affectedRows,
                null,
                null,
                executionTimeMs,
                executedAt,
                sql,
                null,
                null,
                null
        );
    }
    
    /**
     * Create an error result
     */
    public static SqlExecutionResult errorResult(
            String error,
            String sqlState,
            Integer errorCode,
            Long executionTimeMs,
            LocalDateTime executedAt,
            String sql) {
        return new SqlExecutionResult(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                executionTimeMs,
                executedAt,
                sql,
                error,
                sqlState,
                errorCode
        );
    }
}