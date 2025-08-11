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

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SqlParameterExtractorTest {

    @Test
    void testBasicParameterExtraction() {
        String sql = "SELECT * FROM users WHERE name = :userName";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userName"), params);
    }

    @Test
    void testParametersInStringLiteralsAreIgnored() {
        String sql = "SELECT 'hello :name' FROM users WHERE id = :userId";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userId"), params);
    }

    @Test
    void testParametersInDoubleQuotedStringsAreIgnored() {
        String sql = "SELECT \"hello :name\" FROM users WHERE id = :userId";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userId"), params);
    }

    @Test
    void testParametersInLineCommentsAreIgnored() {
        String sql = "SELECT * FROM users WHERE name = :userName -- comment with :param";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userName"), params);
    }

    @Test
    void testParametersInBlockCommentsAreIgnored() {
        String sql = "SELECT * FROM users /* :commentParam */ WHERE id = :userId";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userId"), params);
    }

    @Test
    void testMixedStringLiteralAndRealParameter() {
        String sql = "SELECT ':param1' as col, name FROM users WHERE id = :param2";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("param2"), params);
    }

    @Test
    void testLineCommentWithNewlineAndRealParameter() {
        String sql = "-- This is a comment with :commentParam\nSELECT * FROM users WHERE id = :realParam";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("realParam"), params);
    }

    @Test
    void testMultiLineBlockCommentWithRealParameter() {
        String sql = "/* Multi-line comment\n   with :fakeParam\n*/ SELECT * FROM users WHERE name = :actualParam";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("actualParam"), params);
    }

    @Test
    void testEscapedQuotesInStrings() {
        String sql = "SELECT 'user''s :name' FROM users WHERE id = :userId";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userId"), params);
    }

    @Test
    void testEscapedDoubleQuotesInStrings() {
        String sql = "SELECT \"user\"\"s :name\" FROM users WHERE id = :userId";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userId"), params);
    }

    @Test
    void testMultipleParameters() {
        String sql = "SELECT * FROM users WHERE name = :userName AND age > :minAge AND status = :userStatus";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userName", "minAge", "userStatus"), params);
    }

    @Test
    void testDuplicateParametersReturnOnce() {
        String sql = "SELECT * FROM users WHERE name = :userId OR id = :userId";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userId"), params);
    }

    @Test
    void testParameterWithUnderscores() {
        String sql = "SELECT * FROM users WHERE user_name = :user_name";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("user_name"), params);
    }

    @Test
    void testParameterWithNumbers() {
        String sql = "SELECT * FROM users WHERE id = :param1 AND name = :param2";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("param1", "param2"), params);
    }

    @Test
    void testNoParameters() {
        String sql = "SELECT * FROM users WHERE name = 'John'";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertTrue(params.isEmpty());
    }

    @Test
    void testEmptyString() {
        List<String> params = SqlParameterExtractor.extractParameters("");
        assertTrue(params.isEmpty());
    }

    @Test
    void testNullString() {
        List<String> params = SqlParameterExtractor.extractParameters(null);
        assertTrue(params.isEmpty());
    }

    @Test
    void testComplexQuery() {
        String sql = """
            -- Get user info with :commentedParam
            SELECT u.name, /* :blockParam */ u.email, 
                   ':stringParam' as literal,
                   "another :quotedParam" as quoted_literal
            FROM users u 
            WHERE u.id = :userId 
              AND u.status = :status
              AND u.created_date > :createdAfter
            """;
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        assertEquals(List.of("userId", "status", "createdAfter"), params);
    }

    @Test
    void testUnterminatedString() {
        // Test graceful handling of unterminated string
        String sql = "SELECT * FROM users WHERE name = ':unterminated AND id = :userId";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        // Should not find userId since it's inside an unterminated string
        assertTrue(params.isEmpty());
    }

    @Test
    void testUnterminatedComment() {
        // Test graceful handling of unterminated block comment
        String sql = "SELECT * FROM users /* unterminated comment :param";
        List<String> params = SqlParameterExtractor.extractParameters(sql);
        // Should not find param since it's inside an unterminated comment
        assertTrue(params.isEmpty());
    }
}