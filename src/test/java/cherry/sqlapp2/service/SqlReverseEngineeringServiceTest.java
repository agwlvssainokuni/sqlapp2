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

package cherry.sqlapp2.service;

import cherry.sqlapp2.dto.QueryStructure;
import cherry.sqlapp2.dto.SqlParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SQLリバースエンジニアリングサービステスト")
class SqlReverseEngineeringServiceTest {

    private SqlReverseEngineeringService service;

    @BeforeEach
    void setUp() {
        service = new SqlReverseEngineeringService();
    }

    @Nested
    @DisplayName("GROUP BY句のパース")
    class GroupByParsing {

        @Test
        @DisplayName("単一カラムのGROUP BY句をパースする")
        void shouldParseSingleColumnGroupBy() {
            // Given
            String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            assertThat(structure.getGroupByColumns()).hasSize(1);
            assertThat(structure.getGroupByColumns().get(0).getColumnName()).isEqualTo("department");
            assertThat(structure.getGroupByColumns().get(0).getTableName()).isEmpty();
        }

        @Test
        @DisplayName("複数カラムのGROUP BY句をパースする")
        void shouldParseMultipleColumnGroupBy() {
            // Given
            String sql = "SELECT department, position, COUNT(*) FROM employees GROUP BY department, position";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            assertThat(structure.getGroupByColumns()).hasSize(2);
            assertThat(structure.getGroupByColumns().get(0).getColumnName()).isEqualTo("department");
            assertThat(structure.getGroupByColumns().get(1).getColumnName()).isEqualTo("position");
        }

        @Test
        @DisplayName("テーブル修飾子付きのGROUP BY句をパースする")
        void shouldParseGroupByWithTablePrefix() {
            // Given
            String sql = "SELECT e.department, COUNT(*) FROM employees AS e GROUP BY e.department";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            assertThat(structure.getGroupByColumns()).hasSize(1);
            assertThat(structure.getGroupByColumns().get(0).getTableName()).isEqualTo("e");
            assertThat(structure.getGroupByColumns().get(0).getColumnName()).isEqualTo("department");
        }
    }

    @Nested
    @DisplayName("HAVING句のパース")
    class HavingParsing {

        @Test
        @DisplayName("単純なHAVING句をパースする")
        void shouldParseSimpleHaving() {
            // Given
            String sql = "SELECT department, COUNT(*) as cnt FROM employees GROUP BY department HAVING cnt > 5";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            assertThat(structure.getHavingConditions()).hasSize(1);
            assertThat(structure.getHavingConditions().get(0).getColumnName()).isEqualTo("cnt");
            assertThat(structure.getHavingConditions().get(0).getOperator()).isEqualTo(">");
            assertThat(structure.getHavingConditions().get(0).getValue()).isEqualTo("5");
        }

        @Test
        @DisplayName("複数条件のHAVING句をパースする")
        void shouldParseMultipleHavingConditions() {
            // Given
            String sql = "SELECT department, COUNT(*) as cnt, AVG(salary) as avg_sal FROM employees GROUP BY department HAVING cnt > 5 AND avg_sal > 50000";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            assertThat(structure.getHavingConditions()).hasSize(2);
            
            assertThat(structure.getHavingConditions().get(0).getColumnName()).isEqualTo("cnt");
            assertThat(structure.getHavingConditions().get(0).getOperator()).isEqualTo(">");
            assertThat(structure.getHavingConditions().get(0).getValue()).isEqualTo("5");
            
            assertThat(structure.getHavingConditions().get(1).getColumnName()).isEqualTo("avg_sal");
            assertThat(structure.getHavingConditions().get(1).getOperator()).isEqualTo(">");
            assertThat(structure.getHavingConditions().get(1).getValue()).isEqualTo("50000");
            assertThat(structure.getHavingConditions().get(1).getLogicalOperator()).isEqualTo("AND");
        }

        @Test
        @DisplayName("HAVING句でBETWEEN演算子をパースする")
        void shouldParseHavingWithBetween() {
            // Given
            String sql = "SELECT department, COUNT(*) as cnt FROM employees GROUP BY department HAVING cnt BETWEEN 5 AND 10";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            assertThat(structure.getHavingConditions()).hasSize(1);
            assertThat(structure.getHavingConditions().get(0).getColumnName()).isEqualTo("cnt");
            assertThat(structure.getHavingConditions().get(0).getOperator()).isEqualTo("BETWEEN");
            assertThat(structure.getHavingConditions().get(0).getMinValue()).isEqualTo("5");
            assertThat(structure.getHavingConditions().get(0).getMaxValue()).isEqualTo("10");
        }
    }

    @Nested
    @DisplayName("GROUP BY + HAVING複合句のパース")
    class GroupByHavingCombined {

        @Test
        @DisplayName("GROUP BYとHAVINGを含む複雑なクエリをパースする")
        void shouldParseComplexQueryWithGroupByAndHaving() {
            // Given
            String sql = "SELECT e.department, COUNT(e.id) as emp_count, AVG(e.salary) as avg_sal " +
                        "FROM employees AS e " +
                        "WHERE e.active = 1 " +
                        "GROUP BY e.department " +
                        "HAVING emp_count > 3 AND avg_sal > 60000 " +
                        "ORDER BY avg_sal DESC";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            
            // GROUP BY検証
            assertThat(structure.getGroupByColumns()).hasSize(1);
            assertThat(structure.getGroupByColumns().get(0).getTableName()).isEqualTo("e");
            assertThat(structure.getGroupByColumns().get(0).getColumnName()).isEqualTo("department");
            
            // HAVING検証
            assertThat(structure.getHavingConditions()).hasSize(2);
            assertThat(structure.getHavingConditions().get(0).getColumnName()).isEqualTo("emp_count");
            assertThat(structure.getHavingConditions().get(0).getOperator()).isEqualTo(">");
            assertThat(structure.getHavingConditions().get(0).getValue()).isEqualTo("3");
            
            assertThat(structure.getHavingConditions().get(1).getColumnName()).isEqualTo("avg_sal");
            assertThat(structure.getHavingConditions().get(1).getOperator()).isEqualTo(">");
            assertThat(structure.getHavingConditions().get(1).getValue()).isEqualTo("60000");
            assertThat(structure.getHavingConditions().get(1).getLogicalOperator()).isEqualTo("AND");
            
            // WHERE句も正しく解析されることを確認
            assertThat(structure.getWhereConditions()).hasSize(1);
            assertThat(structure.getWhereConditions().get(0).getTableName()).isEqualTo("e");
            assertThat(structure.getWhereConditions().get(0).getColumnName()).isEqualTo("active");
            assertThat(structure.getWhereConditions().get(0).getValue()).isEqualTo("1");
        }
    }

    @Nested
    @DisplayName("集約関数を含むHAVING・ORDER BY句のパース")
    class AggregateFunctionParsing {

        @Test
        @DisplayName("HAVING句の集約関数をパースする")
        void shouldParseAggregateFunctionInHaving() {
            // Given
            String sql = "SELECT department, COUNT(*) as cnt FROM employees GROUP BY department HAVING COUNT(*) > 10";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            assertThat(structure.getHavingConditions()).hasSize(1);
            
            // 集約関数が正しく解析されることを確認
            QueryStructure.WhereCondition havingCondition = structure.getHavingConditions().get(0);
            assertThat(havingCondition.getAggregateFunction()).isEqualTo("COUNT");
            assertThat(havingCondition.getTableName()).isEmpty();
            assertThat(havingCondition.getColumnName()).isEqualTo("*");
            assertThat(havingCondition.getOperator()).isEqualTo(">");
            assertThat(havingCondition.getValue()).isEqualTo("10");
        }

        @Test
        @DisplayName("ORDER BY句の集約関数をパースする")
        void shouldParseAggregateFunctionInOrderBy() {
            // Given
            String sql = "SELECT department, SUM(salary) as total FROM employees GROUP BY department ORDER BY SUM(salary) DESC";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            assertThat(structure.getOrderByColumns()).hasSize(1);
            
            // 集約関数が正しく解析されることを確認
            QueryStructure.OrderByColumn orderByColumn = structure.getOrderByColumns().get(0);
            assertThat(orderByColumn.getAggregateFunction()).isEqualTo("SUM");
            assertThat(orderByColumn.getTableName()).isEmpty();
            assertThat(orderByColumn.getColumnName()).isEqualTo("salary");
            assertThat(orderByColumn.getDirection()).isEqualTo("DESC");
            
            // SELECT句の集約関数も確認
            assertThat(structure.getSelectColumns()).hasSize(2);
            QueryStructure.SelectColumn aggregateColumn = structure.getSelectColumns().get(1);
            assertThat(aggregateColumn.getAggregateFunction()).isEqualTo("SUM");
            assertThat(aggregateColumn.getTableName()).isEmpty();
            assertThat(aggregateColumn.getColumnName()).isEqualTo("salary");
            assertThat(aggregateColumn.getAlias()).isEqualTo("total");
        }

        @Test
        @DisplayName("複雑な集約関数を含むクエリをパースする")
        void shouldParseComplexQueryWithMultipleAggregateFunctions() {
            // Given
            String sql = "SELECT department, COUNT(*) as emp_count, AVG(salary) as avg_sal " +
                        "FROM employees " +
                        "GROUP BY department " +
                        "HAVING COUNT(*) > 5 AND AVG(salary) > 50000 " +
                        "ORDER BY AVG(salary) DESC, COUNT(*) ASC";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            
            // GROUP BY検証
            assertThat(structure.getGroupByColumns()).hasSize(1);
            assertThat(structure.getGroupByColumns().get(0).getColumnName()).isEqualTo("department");
            
            // HAVING検証（集約関数は現在制限されているため、基本的な構造のみ確認）
            assertThat(structure.getHavingConditions()).hasSize(2);
            
            // ORDER BY検証（集約関数は現在制限されているため、基本的な構造のみ確認）
            assertThat(structure.getOrderByColumns()).hasSize(2);
        }

        @Test
        @DisplayName("報告された問題のSQLをパースする")
        void shouldParseReportedProblemQuery() {
            // Given
            String sql = "SELECT COUNT(v1.id), m1.id FROM tbl_value AS v1 RIGHT JOIN tbl_master AS m1 ON m1.id = v1.field_id AND m1.type = v1.field_type GROUP BY m1.id HAVING COUNT(v1.id) >= '1'";

            // When
            SqlParseResult result = service.parseSQL(sql);

            // Then
            assertThat(result.success()).isTrue();
            QueryStructure structure = result.queryStructure();
            
            // SELECT句の確認（集約関数含む）
            assertThat(structure.getSelectColumns()).hasSize(2);
            
            // 最初のカラム: COUNT(v1.id)
            QueryStructure.SelectColumn firstColumn = structure.getSelectColumns().get(0);
            assertThat(firstColumn.getAggregateFunction()).isEqualTo("COUNT");
            assertThat(firstColumn.getTableName()).isEqualTo("v1");
            assertThat(firstColumn.getColumnName()).isEqualTo("id");
            
            // 2番目のカラム: m1.id
            QueryStructure.SelectColumn secondColumn = structure.getSelectColumns().get(1);
            assertThat(secondColumn.getAggregateFunction()).isNull();
            assertThat(secondColumn.getTableName()).isEqualTo("m1");
            assertThat(secondColumn.getColumnName()).isEqualTo("id");
            
            // JOIN条件の確認
            assertThat(structure.getJoins()).hasSize(1);
            assertThat(structure.getJoins().get(0).getJoinType()).isEqualTo("RIGHT");
            assertThat(structure.getJoins().get(0).getTableName()).isEqualTo("tbl_master");
            assertThat(structure.getJoins().get(0).getAlias()).isEqualTo("m1");
            
            // GROUP BY検証
            assertThat(structure.getGroupByColumns()).hasSize(1);
            assertThat(structure.getGroupByColumns().get(0).getTableName()).isEqualTo("m1");
            assertThat(structure.getGroupByColumns().get(0).getColumnName()).isEqualTo("id");
            
            // HAVING検証（集約関数含む）
            assertThat(structure.getHavingConditions()).hasSize(1);
            QueryStructure.WhereCondition havingCondition = structure.getHavingConditions().get(0);
            assertThat(havingCondition.getAggregateFunction()).isEqualTo("COUNT");
            assertThat(havingCondition.getTableName()).isEqualTo("v1");
            assertThat(havingCondition.getColumnName()).isEqualTo("id");
            assertThat(havingCondition.getOperator()).isEqualTo(">=");
            assertThat(havingCondition.getValue()).isEqualTo("1");
        }
    }
}