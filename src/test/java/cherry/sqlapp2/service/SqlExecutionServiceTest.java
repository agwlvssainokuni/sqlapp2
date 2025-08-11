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
package cherry.sqlapp2.service;

import cherry.sqlapp2.util.SqlParameterExtractor;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for parameter conversion logic used in SqlExecutionService.
 * Tests the core functionality of converting named parameters to positioned parameters
 * while properly handling string literals and comments.
 */
class SqlExecutionServiceTest {

    @Test
    void testParameterConversion_basicCase() {
        String sql = "SELECT * FROM users WHERE id = :userId AND name = :userName";
        
        // Simulate the conversion logic from SqlExecutionService
        List<SqlParameterExtractor.ParameterPosition> positions = 
            SqlParameterExtractor.extractParametersWithPositions(sql);
        String convertedSql = convertSqlWithPositions(sql, positions);
        
        assertThat(convertedSql).isEqualTo("SELECT * FROM users WHERE id = ? AND name = ?");
        assertThat(positions).hasSize(2);
        assertThat(positions.get(0).name()).isEqualTo("userId");
        assertThat(positions.get(1).name()).isEqualTo("userName");
    }

    @Test
    void testParameterConversion_ignoresParametersInStrings() {
        String sql = "SELECT ':userId' as literal, name FROM users WHERE id = :userId";
        
        List<SqlParameterExtractor.ParameterPosition> positions = 
            SqlParameterExtractor.extractParametersWithPositions(sql);
        String convertedSql = convertSqlWithPositions(sql, positions);
        
        assertThat(convertedSql).isEqualTo("SELECT ':userId' as literal, name FROM users WHERE id = ?");
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).name()).isEqualTo("userId");
    }

    @Test
    void testParameterConversion_ignoresParametersInComments() {
        String sql = "SELECT * FROM users -- comment with :commentParam\nWHERE id = :userId";
        
        List<SqlParameterExtractor.ParameterPosition> positions = 
            SqlParameterExtractor.extractParametersWithPositions(sql);
        String convertedSql = convertSqlWithPositions(sql, positions);
        
        assertThat(convertedSql).isEqualTo("SELECT * FROM users -- comment with :commentParam\nWHERE id = ?");
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).name()).isEqualTo("userId");
    }

    @Test
    void testParameterConversion_duplicateParameters() {
        String sql = "SELECT * FROM users WHERE id = :userId OR parent_id = :userId";
        
        List<SqlParameterExtractor.ParameterPosition> positions = 
            SqlParameterExtractor.extractParametersWithPositions(sql);
        String convertedSql = convertSqlWithPositions(sql, positions);
        
        assertThat(convertedSql).isEqualTo("SELECT * FROM users WHERE id = ? OR parent_id = ?");
        assertThat(positions).hasSize(2);
        assertThat(positions.get(0).name()).isEqualTo("userId");
        assertThat(positions.get(1).name()).isEqualTo("userId");
    }

    @Test
    void testParameterConversion_complexMixedScenario() {
        String sql = """
            SELECT * FROM users 
            WHERE name = ':not_a_param' -- comment with :also_not_param
            AND id = :user_id
            AND created_at > :start_date
            /* block comment with :another_fake_param */
            AND status = :status
            """;
        
        List<SqlParameterExtractor.ParameterPosition> positions = 
            SqlParameterExtractor.extractParametersWithPositions(sql);
        String convertedSql = convertSqlWithPositions(sql, positions);
        
        assertThat(convertedSql).contains("WHERE name = ':not_a_param'");
        assertThat(convertedSql).contains("-- comment with :also_not_param");
        assertThat(convertedSql).contains("/* block comment with :another_fake_param */");
        assertThat(convertedSql).contains("AND id = ?");
        assertThat(convertedSql).contains("AND created_at > ?");
        assertThat(convertedSql).contains("AND status = ?");
        
        assertThat(positions).hasSize(3);
        assertThat(positions.get(0).name()).isEqualTo("user_id");
        assertThat(positions.get(1).name()).isEqualTo("start_date");
        assertThat(positions.get(2).name()).isEqualTo("status");
    }

    /**
     * Helper method to simulate the parameter conversion logic from SqlExecutionService.
     * This replicates the exact logic used in convertNamedParameters method.
     */
    private String convertSqlWithPositions(String sql, List<SqlParameterExtractor.ParameterPosition> positions) {
        StringBuilder convertedSqlBuilder = new StringBuilder(sql);
        
        // Process parameters in reverse order to maintain position accuracy
        for (int i = positions.size() - 1; i >= 0; i--) {
            SqlParameterExtractor.ParameterPosition paramPos = positions.get(i);
            convertedSqlBuilder.replace(paramPos.start(), paramPos.end(), "?");
        }
        
        return convertedSqlBuilder.toString();
    }
}