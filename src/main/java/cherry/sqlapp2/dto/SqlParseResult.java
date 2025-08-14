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

/**
 * Result of SQL parsing operation for reverse engineering.
 * Contains either successfully parsed QueryStructure or error information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SqlParseResult(
    boolean success,
    QueryStructure queryStructure,
    String errorMessage,
    String originalSql
) {
    
    /**
     * Create a successful parse result.
     */
    public static SqlParseResult success(QueryStructure queryStructure) {
        return new SqlParseResult(true, queryStructure, null, null);
    }
    
    /**
     * Create a successful parse result with original SQL.
     */
    public static SqlParseResult success(QueryStructure queryStructure, String originalSql) {
        return new SqlParseResult(true, queryStructure, null, originalSql);
    }
    
    /**
     * Create an error result.
     */
    public static SqlParseResult error(String errorMessage) {
        return new SqlParseResult(false, null, errorMessage, null);
    }
    
    /**
     * Create an error result with original SQL.
     */
    public static SqlParseResult error(String errorMessage, String originalSql) {
        return new SqlParseResult(false, null, errorMessage, originalSql);
    }
}