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
 * SQLクエリ検証APIのレスポンス
 */
public record SqlValidationResult(
        boolean ok,
        LocalDateTime validatedAt,
        String sql,
        String error,
        String errorType
) {
    // 成功時のコンストラクタ
    public static SqlValidationResult validationOk(LocalDateTime validatedAt, String sql) {
        return new SqlValidationResult(true, validatedAt, sql, null, null);
    }

    // エラー時のコンストラクタ
    public static SqlValidationResult validationNg(LocalDateTime validatedAt, String sql, String error, String errorType) {
        return new SqlValidationResult(false, validatedAt, sql, error, errorType);
    }
}
