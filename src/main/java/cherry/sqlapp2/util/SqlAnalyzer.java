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
package cherry.sqlapp2.util;

import java.util.regex.Pattern;

/**
 * SQLの解析機能を提供するユーティリティクラス。
 * ページング機能とクエリ構造検出のためのSQL解析を行います。
 * SELECT文の検証、LIMIT/OFFSET句の検出、ORDER BY句の確認などを提供します。
 */
public class SqlAnalyzer {

    // Regex patterns for SQL clause detection (case-insensitive)
    private static final Pattern LIMIT_PATTERN = Pattern.compile(
            "\\blimit\\s+\\d+", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern OFFSET_PATTERN = Pattern.compile(
            "\\boffset\\s+\\d+", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile(
            "\\border\\s+by\\s+", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "^\\s*select\\s+", 
            Pattern.CASE_INSENSITIVE
    );

    /**
     * SQLにLIMIT句が含まれているかをチェックします。
     * 
     * @param sql チェック対象のSQL文
     * @return LIMIT句が含まれている場合true
     */
    public static boolean hasLimitClause(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        return LIMIT_PATTERN.matcher(sql).find();
    }

    /**
     * SQLにOFFSET句が含まれているかをチェックします。
     * 
     * @param sql チェック対象のSQL文
     * @return OFFSET句が含まれている場合true
     */
    public static boolean hasOffsetClause(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        return OFFSET_PATTERN.matcher(sql).find();
    }

    /**
     * SQLにページング競合（LIMITまたはOFFSET）があるかをチェックします。
     * 
     * @param sql チェック対象のSQL文
     * @return ページング競合がある場合true
     */
    public static boolean hasPagingConflict(String sql) {
        return hasLimitClause(sql) || hasOffsetClause(sql);
    }

    /**
     * Check if SQL contains ORDER BY clause (recommended for pagination)
     * SQLにORDER BY句が含まれているかチェック（ページングに推奨）
     */
    public static boolean hasOrderByClause(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        return ORDER_BY_PATTERN.matcher(sql).find();
    }

    /**
     * SQLがSELECTクエリかをチェックします。
     * SELECT、WITH、SHOW、DESCRIBE、EXPLAIN文を検出します。
     * 
     * @param sql チェック対象のSQL文
     * @return SELECTクエリの場合true
     */
    public static boolean isSelectQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String trimmedSql = sql.trim().toLowerCase();
        return trimmedSql.startsWith("select") ||
               trimmedSql.startsWith("with") ||
               trimmedSql.startsWith("show") ||
               trimmedSql.startsWith("describe") ||
               trimmedSql.startsWith("desc") ||
               trimmedSql.startsWith("explain");
    }

    /**
     * Check if SQL is suitable for pagination
     * SQLがページングに適しているかチェック
     */
    public static boolean isSuitableForPaging(String sql) {
        return isSelectQuery(sql) && 
               !hasPagingConflict(sql) && 
               hasOrderByClause(sql);
    }

    /**
     * Get pagination compatibility status
     * ページング互換性ステータス取得
     */
    public static PagingCompatibility getPagingCompatibility(String sql) {
        if (!isSelectQuery(sql)) {
            return PagingCompatibility.NOT_SELECT;
        }
        
        if (hasPagingConflict(sql)) {
            return PagingCompatibility.HAS_LIMIT_OFFSET;
        }
        
        if (!hasOrderByClause(sql)) {
            return PagingCompatibility.NO_ORDER_BY;
        }
        
        return PagingCompatibility.COMPATIBLE;
    }

    /**
     * Pagination compatibility status enum
     * ページング互換性ステータス列挙型
     */
    public enum PagingCompatibility {
        COMPATIBLE("Compatible with pagination"),
        NOT_SELECT("Not a SELECT query"),
        HAS_LIMIT_OFFSET("Already contains LIMIT/OFFSET clause"),
        NO_ORDER_BY("No ORDER BY clause (results may be inconsistent)");

        private final String description;

        PagingCompatibility(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isCompatible() {
            return this == COMPATIBLE;
        }

        public boolean allowsWarning() {
            return this == NO_ORDER_BY;
        }
    }
}