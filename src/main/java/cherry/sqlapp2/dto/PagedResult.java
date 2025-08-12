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
 * Paged result container for large datasets
 * 大容量データセット用ページング結果コンテナ
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagedResult<T>(
        List<T> data,
        int page,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> PagedResult<T> of(List<T> data, int page, int pageSize, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;
        
        return new PagedResult<>(
                data,
                page,
                pageSize,
                totalElements,
                totalPages,
                hasNext,
                hasPrevious
        );
    }
}