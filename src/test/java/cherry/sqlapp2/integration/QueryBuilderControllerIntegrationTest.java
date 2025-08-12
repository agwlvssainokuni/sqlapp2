/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.sqlapp2.integration;

import cherry.sqlapp2.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QueryBuilderController統合テスト
 * クエリ構築コントローラーの動作を検証
 */
@DisplayName("QueryBuilderController - クエリ構築コントローラー統合テスト")
public class QueryBuilderControllerIntegrationTest extends BaseIntegrationTest {

    /**
     * COUNT(*)関数を持つSelectColumnを作成
     */
    private QueryStructure.SelectColumn createCountColumn() {
        var countColumn = new QueryStructure.SelectColumn(null, "*", "COUNT");
        countColumn.setAlias("count");
        return countColumn;
    }

    @Nested
    @DisplayName("SQL構築")
    class QueryBuilding {

        @Test
        @DisplayName("基本的なSELECTクエリを構築できる")
        void shouldBuildBasicSelectQuery() throws Exception {
            // テスト用のクエリ構造
            var queryStructure = new QueryStructure();
            queryStructure.setSelectColumns(Arrays.asList(
                new QueryStructure.SelectColumn(null, "id"),
                new QueryStructure.SelectColumn(null, "name"),
                new QueryStructure.SelectColumn(null, "email")
            ));
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("users")
            ));

            var request = new QueryBuilderRequest(queryStructure);
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.generatedSql").isString())
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString("SELECT")))
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString("FROM users")));
        }

        @Test
        @DisplayName("WHERE条件付きクエリを構築できる")
        void shouldBuildQueryWithWhereClause() throws Exception {
            var queryStructure = new QueryStructure();
            queryStructure.setSelectColumns(Arrays.asList(
                new QueryStructure.SelectColumn(null, "id"),
                new QueryStructure.SelectColumn(null, "name")
            ));
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("users")
            ));
            queryStructure.setWhereConditions(Arrays.asList(
                new QueryStructure.WhereCondition(null, "age", ">=", ":minAge"),
                new QueryStructure.WhereCondition(null, "status", "=", ":activeStatus")
            ));

            var request = new QueryBuilderRequest(queryStructure);
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString("WHERE")))
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString(":minAge")))
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString(":activeStatus")));
        }

        @Test
        @DisplayName("JOIN句を含む複雑なクエリを構築できる")
        void shouldBuildComplexQueryWithJoin() throws Exception {
            var queryStructure = new QueryStructure();
            queryStructure.setSelectColumns(Arrays.asList(
                new QueryStructure.SelectColumn("u", "id"),
                new QueryStructure.SelectColumn("u", "name"),
                new QueryStructure.SelectColumn("p", "title")
            ));
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("users", "u")
            ));
            
            var joinClause = new QueryStructure.JoinClause("INNER JOIN", "posts");
            joinClause.setAlias("p");
            joinClause.setConditions(Arrays.asList(
                new QueryStructure.JoinClause.JoinCondition("u", "id", "=", "p", "user_id")
            ));
            queryStructure.setJoins(Arrays.asList(joinClause));

            var request = new QueryBuilderRequest(queryStructure);
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString("INNER JOIN")))
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString("posts")));
        }

        @Test
        @DisplayName("GROUP BY / ORDER BY句を含むクエリを構築できる")
        void shouldBuildQueryWithGroupByAndOrderBy() throws Exception {
            var queryStructure = new QueryStructure();
            queryStructure.setSelectColumns(Arrays.asList(
                new QueryStructure.SelectColumn(null, "department"),
                createCountColumn()
            ));
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("employees")
            ));
            queryStructure.setGroupByColumns(Arrays.asList(
                new QueryStructure.GroupByColumn(null, "department")
            ));
            queryStructure.setOrderByColumns(Arrays.asList(
                new QueryStructure.OrderByColumn(null, "count", "DESC"),
                new QueryStructure.OrderByColumn(null, "department", "ASC")
            ));

            var request = new QueryBuilderRequest(queryStructure);
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString("GROUP BY")))
                .andExpect(jsonPath("$.data.generatedSql").value(org.hamcrest.Matchers.containsString("ORDER BY")));
        }
    }

    @Nested
    @DisplayName("バリデーション")
    class Validation {

        @Test
        @DisplayName("空のクエリ構造でバリデーションエラーが返される")
        void shouldReturnValidationErrorForEmptyStructure() throws Exception {
            var request = new QueryBuilderRequest(); // queryStructureがnull
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("不正なクエリ構造でバリデーションエラーが返される")
        void shouldReturnValidationErrorForInvalidStructure() throws Exception {
            // SELECT句なしの不正な構造
            var queryStructure = new QueryStructure();
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("users")
            )); // SELECT句がない

            var request = new QueryBuilderRequest(queryStructure);
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(jsonPath("$.data.validationErrors").exists());
        }
    }

    @Nested
    @DisplayName("オプション設定")
    class Options {

        @Test
        @DisplayName("SQLフォーマット無効時に非整形SQLが返される")
        void shouldReturnUnformattedSqlWhenFormattingDisabled() throws Exception {
            var queryStructure = new QueryStructure();
            queryStructure.setSelectColumns(Arrays.asList(
                new QueryStructure.SelectColumn(null, "id"),
                new QueryStructure.SelectColumn(null, "name")
            ));
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("users")
            ));

            var request = new QueryBuilderRequest(queryStructure);
            request.setFormatSql(false); // フォーマット無効
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedSql").isString());
        }

        @Test
        @DisplayName("コメント有効時にSQLコメントが含まれる")
        void shouldIncludeCommentsWhenEnabled() throws Exception {
            var queryStructure = new QueryStructure();
            queryStructure.setSelectColumns(Arrays.asList(
                new QueryStructure.SelectColumn(null, "id"),
                new QueryStructure.SelectColumn(null, "name")
            ));
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("users")
            ));

            var request = new QueryBuilderRequest(queryStructure);
            request.setIncludeComments(true); // コメント有効
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedSql").isString());
        }
    }

    @Nested
    @DisplayName("セキュリティ")
    class Security {

        @Test
        @DisplayName("認証なしでアクセス時403エラーが返される")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            var queryStructure = new QueryStructure();
            queryStructure.setSelectColumns(Arrays.asList(
                new QueryStructure.SelectColumn(null, "id"),
                new QueryStructure.SelectColumn(null, "name")
            ));
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("users")
            ));

            var request = new QueryBuilderRequest(queryStructure);

            mockMvc.perform(post("/api/query-builder/build")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("無効なJWTトークンで403エラーが返される")
        void shouldReturn403WithInvalidJwtToken() throws Exception {
            var queryStructure = new QueryStructure();
            queryStructure.setSelectColumns(Arrays.asList(
                new QueryStructure.SelectColumn(null, "id"),
                new QueryStructure.SelectColumn(null, "name")
            ));
            queryStructure.setFromTables(Arrays.asList(
                new QueryStructure.FromTable("users")
            ));

            var request = new QueryBuilderRequest(queryStructure);

            mockMvc.perform(post("/api/query-builder/build")
                        .header("Authorization", "Bearer invalid-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
        }
    }
}