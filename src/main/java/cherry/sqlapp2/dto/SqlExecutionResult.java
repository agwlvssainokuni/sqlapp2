/*
 * Copyright 2024 sqlapp2 project
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

/**
 * SQL実行結果のレスポンス
 */
public record SqlExecutionResult(
        boolean ok,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDateTime executedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long executionTime,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String sql,
        // SQL実行結果データ（SELECT文の場合）
        @JsonInclude(JsonInclude.Include.NON_NULL)
        SqlResultData data,
        // エラー情報
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String error,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String errorType,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer errorCode,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String sqlState,
        // 実行履歴・保存クエリID
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long queryHistoryId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long savedQueryId
) {

    // 検証OKレスポンス用コンストラクタ
    public static SqlExecutionResult validationOk(LocalDateTime validatedAt, String sql) {
        return new SqlExecutionResult(true, validatedAt, null, sql, null, null, null, null, null, null, null);
    }

    // 検証NGレスポンス用コンストラクタ
    public static SqlExecutionResult validationNg(LocalDateTime validatedAt, String sql, String error, String errorType) {
        return new SqlExecutionResult(false, validatedAt, null, sql, null, error, errorType, null, null, null, null);
    }

    // SQL実行成功レスポンス用コンストラクタ
    public static SqlExecutionResult success(LocalDateTime executedAt, Long executionTime, String sql, SqlResultData data, Long queryHistoryId, Long savedQueryId) {
        return new SqlExecutionResult(true, executedAt, executionTime, sql, data, null, null, null, null, queryHistoryId, savedQueryId);
    }

    // エラーレスポンス用コンストラクタ
    public static SqlExecutionResult error(LocalDateTime executedAt, String sql, String error, String errorType, Integer errorCode, String sqlState, Long queryHistoryId, Long savedQueryId) {
        return new SqlExecutionResult(false, executedAt, null, sql, null, error, errorType, errorCode, sqlState, queryHistoryId, savedQueryId);
    }

    public record ColumnDetail(
            String name,
            String label,
            String type,
            String className,
            boolean nullable,
            int precision,
            int scale
    ) {
    }

    public record SqlResultData(
            List<String> columns,
            List<ColumnDetail> columnDetails,
            List<List<Object>> rows,
            int rowCount,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            PagedResult<List<Object>> paging
    ) {
        // Constructor for non-paged results (backward compatibility)
        public SqlResultData(List<String> columns, List<ColumnDetail> columnDetails, 
                           List<List<Object>> rows, int rowCount) {
            this(columns, columnDetails, rows, rowCount, null);
        }
    }
}
