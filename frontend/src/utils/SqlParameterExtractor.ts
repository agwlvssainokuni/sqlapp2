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

/**
 * TypeScript port of SqlParameterExtractor from the Java backend.
 * Utility for extracting named parameters from SQL queries while properly handling
 * string literals and comments to avoid false positives.
 * 
 * This solves the issue where simple regex patterns like /:(\w+)/g would
 * incorrectly match parameter-like patterns inside:
 * - String literals: SELECT ':param' FROM table
 * - Line comments: -- comment with :param
 * - Block comments: comment with :param
 * 
 * The extractor uses state-based parsing to distinguish between actual SQL code and
 * quoted content, ensuring only legitimate parameters are extracted.
 */

/**
 * Extract named parameters (:paramName) from SQL text, ignoring parameters that appear
 * inside string literals or comments.
 * 
 * @param sql The SQL query text
 * @returns Array of parameter names in order of appearance (without duplicates)
 */
export function extractParameters(sql: string): string[] {
    if (!sql) {
        return []
    }

    const parameterSet = new Set<string>() // Avoids duplicates
    const parameters: string[] = []
    const length = sql.length
    let i = 0
    
    while (i < length) {
        const c = sql.charAt(i)
        
        // Handle string literals (single quotes)
        if (c === "'") {
            i = skipSingleQuotedString(sql, i)
            continue
        }
        
        // Handle string literals (double quotes) 
        if (c === '"') {
            i = skipDoubleQuotedString(sql, i)
            continue
        }
        
        // Handle line comments
        if (c === '-' && i + 1 < length && sql.charAt(i + 1) === '-') {
            i = skipLineComment(sql, i)
            continue
        }
        
        // Handle block comments
        if (c === '/' && i + 1 < length && sql.charAt(i + 1) === '*') {
            i = skipBlockComment(sql, i)
            continue
        }
        
        // Look for parameters in actual SQL code
        if (c === ':' && i + 1 < length && isLetter(sql.charAt(i + 1))) {
            const paramName = extractParameterName(sql, i + 1)
            if (paramName && !parameterSet.has(paramName)) {
                parameterSet.add(paramName)
                parameters.push(paramName)
            }
            i += paramName ? paramName.length + 1 : 1
            continue
        }
        
        i++
    }
    
    return parameters
}

/**
 * Skip over a single-quoted string literal, handling escaped quotes.
 */
function skipSingleQuotedString(sql: string, start: number): number {
    let i = start + 1 // Skip opening quote
    while (i < sql.length) {
        const c = sql.charAt(i)
        if (c === "'") {
            // Check for escaped quote '' (SQL standard)
            if (i + 1 < sql.length && sql.charAt(i + 1) === "'") {
                i += 2 // Skip escaped quote
            } else {
                return i + 1 // End of string
            }
        } else {
            i++
        }
    }
    return i // Unclosed string (end of input)
}

/**
 * Skip over a double-quoted string literal, handling escaped quotes.
 */
function skipDoubleQuotedString(sql: string, start: number): number {
    let i = start + 1 // Skip opening quote
    while (i < sql.length) {
        const c = sql.charAt(i)
        if (c === '"') {
            // Check for escaped quote "" (SQL standard)
            if (i + 1 < sql.length && sql.charAt(i + 1) === '"') {
                i += 2 // Skip escaped quote
            } else {
                return i + 1 // End of string
            }
        } else {
            i++
        }
    }
    return i // Unclosed string (end of input)
}

/**
 * Skip over a line comment (-- comment).
 */
function skipLineComment(sql: string, start: number): number {
    let i = start + 2 // Skip --
    while (i < sql.length && sql.charAt(i) !== '\n') {
        i++
    }
    return i < sql.length ? i + 1 : i // Skip newline if present
}

/**
 * Skip over a block comment.
 */
function skipBlockComment(sql: string, start: number): number {
    let i = start + 2 // Skip /*
    while (i < sql.length - 1) {
        if (sql.charAt(i) === '*' && sql.charAt(i + 1) === '/') {
            return i + 2 // Skip */
        }
        i++
    }
    return sql.length // Unclosed comment (end of input)
}

/**
 * Check if character is a letter (a-z, A-Z).
 */
function isLetter(c: string): boolean {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
}

/**
 * Check if character is alphanumeric or underscore.
 */
function isAlphaNumericUnderscore(c: string): boolean {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c === '_'
}

/**
 * Extract parameter name starting at the given position.
 */
function extractParameterName(sql: string, start: number): string | null {
    let paramName = ''
    let i = start
    
    while (i < sql.length) {
        const c = sql.charAt(i)
        if (isAlphaNumericUnderscore(c)) {
            paramName += c
            i++
        } else {
            break
        }
    }
    
    return paramName.length > 0 ? paramName : null
}