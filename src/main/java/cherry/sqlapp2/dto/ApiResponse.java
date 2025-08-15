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

import java.util.List;

/**
 * API統一レスポンス形式を表すDTOクラス。
 * すべてのAPIエンドポイントで一貫したレスポンス構造を提供します。
 * 成功時はdata、失敗時はerrorリストを含みます。
 * 
 * @param <T> レスポンスデータの型
 */
public record ApiResponse<T>(
        boolean ok,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<String> error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(List<String> error) {
        return new ApiResponse<>(false, null, error);
    }
}
