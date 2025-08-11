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

import java.time.LocalDateTime;

/**
 * SQL実行結果のレスポンス
 */
public record SqlExecutionResult(
        Boolean success,
        String message,
        LocalDateTime executedAt,
        String sql,
        // SQL実行結果データ（SELECT文の場合）
        SqlResultData data,
        // エラー情報
        String error,
        String errorType,
        Integer errorCode,
        String sqlState,
        // 実行履歴・保存クエリID
        Long queryHistoryId,
        Long savedQueryId
) {
    // 検証のみ成功レスポンス用コンストラクタ
    public SqlExecutionResult(Boolean valid, String message, LocalDateTime validatedAt) {
        this(valid, message, validatedAt, null, null, null, null, null, null, null, null);
    }
    
    // SQL実行成功レスポンス用コンストラクタ
    public SqlExecutionResult(Boolean success, SqlResultData data, LocalDateTime executedAt, String sql, Long queryHistoryId, Long savedQueryId) {
        this(success, "Query executed successfully", executedAt, sql, data, null, null, null, null, queryHistoryId, savedQueryId);
    }
    
    // エラーレスポンス用コンストラクタ
    public SqlExecutionResult(Boolean success, String error, String errorType, LocalDateTime executedAt, String sql, Integer errorCode, String sqlState) {
        this(success, error, executedAt, sql, null, error, errorType, errorCode, sqlState, null, null);
    }
    
    public record SqlResultData(
        java.util.List<String> columns,
        java.util.List<java.util.List<Object>> rows,
        Integer rowCount,
        Long executionTime
    ) {}
}