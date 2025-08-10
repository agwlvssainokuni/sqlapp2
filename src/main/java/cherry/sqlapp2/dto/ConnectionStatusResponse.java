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
 * データベース接続ステータスAPIのレスポンス
 */
public record ConnectionStatusResponse(
        Long connectionId,
        Boolean available,
        String error,
        LocalDateTime checkedAt
) {
    // エラーなしの場合のコンストラクタ
    public ConnectionStatusResponse(Long connectionId, Boolean available, LocalDateTime checkedAt) {
        this(connectionId, available, null, checkedAt);
    }
}