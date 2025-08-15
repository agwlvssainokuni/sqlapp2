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
package cherry.sqlapp2.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SQLクエリから名前付きパラメータを抽出するユーティリティクラス。
 * 文字列リテラルやコメント内のパラメータを適切に除外し、誤検出を防ぎます。
 * 
 * <p>このクラスは、単純な正規表現パターン {@code :(\\w+)} では以下の場所で
 * パラメータ様パターンを誤って検出してしまう問題を解決します：</p>
 * <ul>
 *   <li>文字列リテラル: {@code SELECT ':param' FROM table}</li>
 *   <li>行コメント: {@code -- comment with :param}</li>
 *   <li>ブロックコメント: {@code /* comment with :param */}</li>
 * </ul>
 * 
 * <p>このエクストラクタは状態ベースの解析を使用して実際のSQLコードと
 * 引用符内のコンテンツを区別し、正当なパラメータのみを抽出することを保証します。</p>
 */
public class SqlParameterExtractor {

    /**
     * SQLテキストから名前付きパラメータ（:paramName）を抽出します。
     * 文字列リテラルやコメント内のパラメータは無視されます。
     * 
     * @param sql SQLクエリテキスト
     * @return 出現順のパラメータ名リスト（重複なし）
     */
    public static List<String> extractParameters(String sql) {
        if (sql == null || sql.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> parameterSet = new LinkedHashSet<>(); // Preserves order, avoids duplicates
        List<String> parameters = new ArrayList<>();
        int length = sql.length();
        int i = 0;
        
        while (i < length) {
            char c = sql.charAt(i);
            
            // Handle string literals (single quotes)
            if (c == '\'') {
                i = skipSingleQuotedString(sql, i);
                continue;
            }
            
            // Handle string literals (double quotes) 
            if (c == '"') {
                i = skipDoubleQuotedString(sql, i);
                continue;
            }
            
            // Handle line comments
            if (c == '-' && i + 1 < length && sql.charAt(i + 1) == '-') {
                i = skipLineComment(sql, i);
                continue;
            }
            
            // Handle block comments
            if (c == '/' && i + 1 < length && sql.charAt(i + 1) == '*') {
                i = skipBlockComment(sql, i);
                continue;
            }
            
            // Look for parameters in actual SQL code
            if (c == ':' && i + 1 < length && Character.isLetter(sql.charAt(i + 1))) {
                String paramName = extractParameterName(sql, i + 1);
                if (paramName != null && !parameterSet.contains(paramName)) {
                    parameterSet.add(paramName);
                    parameters.add(paramName);
                }
                i += paramName != null ? paramName.length() + 1 : 1;
                continue;
            }
            
            i++;
        }
        
        return parameters;
    }

    /**
     * SQLテキストから名前付きパラメータとその位置情報を抽出します。
     * 文字列リテラルやコメント内のパラメータは無視されます。
     * 
     * @param sql SQLクエリテキスト
     * @return パラメータ名と位置情報を含むParameterPositionオブジェクトのリスト
     */
    public static List<ParameterPosition> extractParametersWithPositions(String sql) {
        if (sql == null || sql.isEmpty()) {
            return new ArrayList<>();
        }

        List<ParameterPosition> parameters = new ArrayList<>();
        int length = sql.length();
        int i = 0;
        
        while (i < length) {
            char c = sql.charAt(i);
            
            // Handle string literals (single quotes)
            if (c == '\'') {
                i = skipSingleQuotedString(sql, i);
                continue;
            }
            
            // Handle string literals (double quotes) 
            if (c == '"') {
                i = skipDoubleQuotedString(sql, i);
                continue;
            }
            
            // Handle line comments
            if (c == '-' && i + 1 < length && sql.charAt(i + 1) == '-') {
                i = skipLineComment(sql, i);
                continue;
            }
            
            // Handle block comments
            if (c == '/' && i + 1 < length && sql.charAt(i + 1) == '*') {
                i = skipBlockComment(sql, i);
                continue;
            }
            
            // Look for parameters in actual SQL code
            if (c == ':' && i + 1 < length && Character.isLetter(sql.charAt(i + 1))) {
                String paramName = extractParameterName(sql, i + 1);
                if (paramName != null) {
                    parameters.add(new ParameterPosition(paramName, i, i + paramName.length() + 1));
                }
                i += paramName != null ? paramName.length() + 1 : 1;
                continue;
            }
            
            i++;
        }
        
        return parameters;
    }

    /**
     * パラメータ名と位置情報を保持するレコードクラス。
     * 
     * @param name パラメータ名
     * @param start 開始位置
     * @param end 終了位置
     */
    public static record ParameterPosition(String name, int start, int end) {
    }

    /**
     * Skip over a single-quoted string literal, handling escaped quotes.
     */
    private static int skipSingleQuotedString(String sql, int start) {
        int i = start + 1; // Skip opening quote
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'') {
                // Check for escaped quote '' (SQL standard)
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i += 2; // Skip escaped quote
                } else {
                    return i + 1; // End of string
                }
            } else {
                i++;
            }
        }
        return i; // Unclosed string (end of input)
    }

    /**
     * Skip over a double-quoted string literal, handling escaped quotes.
     */
    private static int skipDoubleQuotedString(String sql, int start) {
        int i = start + 1; // Skip opening quote
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '"') {
                // Check for escaped quote "" (SQL standard)
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '"') {
                    i += 2; // Skip escaped quote
                } else {
                    return i + 1; // End of string
                }
            } else {
                i++;
            }
        }
        return i; // Unclosed string (end of input)
    }

    /**
     * Skip over a line comment (-- comment).
     */
    private static int skipLineComment(String sql, int start) {
        int i = start + 2; // Skip --
        while (i < sql.length() && sql.charAt(i) != '\n') {
            i++;
        }
        return i < sql.length() ? i + 1 : i; // Skip newline if present
    }

    /**
     * Skip over a block comment (block comment).
     */
    private static int skipBlockComment(String sql, int start) {
        int i = start + 2; // Skip /*
        while (i < sql.length() - 1) {
            if (sql.charAt(i) == '*' && sql.charAt(i + 1) == '/') {
                return i + 2; // Skip */
            }
            i++;
        }
        return sql.length(); // Unclosed comment (end of input)
    }

    /**
     * Extract parameter name starting at the given position.
     */
    private static String extractParameterName(String sql, int start) {
        StringBuilder paramName = new StringBuilder();
        int i = start;
        
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                paramName.append(c);
                i++;
            } else {
                break;
            }
        }
        
        return paramName.length() > 0 ? paramName.toString() : null;
    }
}