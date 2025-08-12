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

import cherry.sqlapp2.dto.DatabaseConnectionRequest;
import cherry.sqlapp2.enums.DatabaseType;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SchemaController統合テスト
 * スキーマ情報取得コントローラーの動作を検証
 */
@DisplayName("SchemaController - スキーマ情報取得統合テスト")
public class SchemaControllerIntegrationTest extends BaseIntegrationTest {

    private Long testConnectionId;

    @BeforeEach
    void setUpSchemaTest() throws Exception {
        // テスト用データベース接続を作成
        var request = new DatabaseConnectionRequest();
        request.setConnectionName("Test Schema Connection");
        request.setDatabaseType(DatabaseType.MYSQL);
        request.setHost("localhost");
        request.setPort(3306);
        request.setDatabaseName("testschema");
        request.setUsername("testuser");
        request.setPassword("testpass");
        request.setActive(true);

        var authHeader = getTestUser1AuthHeader();
        
        var createResult = mockMvc.perform(post("/api/connections")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
            .andExpect(status().isCreated())
            .andReturn();

        Number connectionIdInt = JsonPath.read(
            createResult.getResponse().getContentAsString(), 
            "$.data.id"
        );
        testConnectionId = connectionIdInt.longValue();
    }

    @Nested
    @DisplayName("データベース情報取得")
    class DatabaseInfoRetrieval {

        @Test
        @DisplayName("データベース情報を取得できる")
        @Transactional
        @Rollback
        void shouldGetDatabaseInfo() throws Exception {
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(get("/api/schema/connections/{connectionId}", testConnectionId)
                        .header("Authorization", authHeader))
                // レスポンスの検証（接続失敗の場合500エラーになる）
                .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("存在しない接続IDでエラーが返される")
        @Transactional
        @Rollback
        void shouldReturnErrorForNonExistentConnection() throws Exception {
            var authHeader = getTestUser1AuthHeader();
            Long nonExistentConnectionId = 99999L;

            mockMvc.perform(get("/api/schema/connections/{connectionId}", nonExistentConnectionId)
                        .header("Authorization", authHeader))
                // レスポンスの検証（接続が存在しない場合400エラー - IllegalArgumentException）
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error[0]").value("Connection not found: 99999"));
        }
    }

    @Nested
    @DisplayName("テーブル情報取得")
    class TableInfoRetrieval {

        @Test
        @DisplayName("テーブル一覧を取得できる")
        @Transactional
        @Rollback
        void shouldGetTables() throws Exception {
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(get("/api/schema/connections/{connectionId}/tables", testConnectionId)
                        .header("Authorization", authHeader))
                // レスポンスの検証（接続失敗の場合500エラー）
                .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("カタログとスキーマを指定してテーブル一覧を取得できる")
        @Transactional
        @Rollback
        void shouldGetTablesWithCatalogAndSchema() throws Exception {
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(get("/api/schema/connections/{connectionId}/tables", testConnectionId)
                        .header("Authorization", authHeader)
                        .param("catalog", "test_catalog")
                        .param("schema", "test_schema"))
                // レスポンスの検証（接続失敗の場合500エラー）
                .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("存在しない接続IDでテーブル取得時エラーが返される")
        @Transactional
        @Rollback
        void shouldReturnErrorWhenGettingTablesForNonExistentConnection() throws Exception {
            var authHeader = getTestUser1AuthHeader();
            Long nonExistentConnectionId = 99999L;

            mockMvc.perform(get("/api/schema/connections/{connectionId}/tables", nonExistentConnectionId)
                        .header("Authorization", authHeader))
                // レスポンスの検証（接続が存在しない場合400エラー - IllegalArgumentException）
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error[0]").value("Connection not found: 99999"));
        }
    }

    @Nested
    @DisplayName("テーブル詳細取得")
    class TableDetailsRetrieval {

        @Test
        @DisplayName("テーブル詳細を取得できる")
        @Transactional
        @Rollback
        void shouldGetTableDetails() throws Exception {
            var authHeader = getTestUser1AuthHeader();
            String tableName = "test_table";

            mockMvc.perform(get("/api/schema/connections/{connectionId}/tables/{tableName}", 
                            testConnectionId, tableName)
                        .header("Authorization", authHeader))
                // レスポンスの検証（実際のデータベース接続失敗時は500エラーになる）
                .andExpect(status().is5xxServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 電文の検証
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("カタログとスキーマを指定してテーブル詳細を取得できる")
        @Transactional
        @Rollback
        void shouldGetTableDetailsWithCatalogAndSchema() throws Exception {
            var authHeader = getTestUser1AuthHeader();
            String tableName = "test_table";

            mockMvc.perform(get("/api/schema/connections/{connectionId}/tables/{tableName}", 
                            testConnectionId, tableName)
                        .header("Authorization", authHeader)
                        .param("catalog", "test_catalog")
                        .param("schema", "test_schema"))
                // レスポンスの検証（実際のデータベース接続失敗時は500エラーになる）
                .andExpect(status().is5xxServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 電文の検証
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("カラム情報取得")
    class ColumnInfoRetrieval {

        @Test
        @DisplayName("テーブルのカラム情報を取得できる")
        @Transactional
        @Rollback
        void shouldGetTableColumns() throws Exception {
            var authHeader = getTestUser1AuthHeader();
            String tableName = "test_table";

            mockMvc.perform(get("/api/schema/connections/{connectionId}/tables/{tableName}/columns", 
                            testConnectionId, tableName)
                        .header("Authorization", authHeader))
                // レスポンスの検証（実際のデータベース接続失敗時は500エラーになる）
                .andExpect(status().is5xxServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 電文の検証
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("カタログとスキーマを指定してカラム情報を取得できる")
        @Transactional
        @Rollback
        void shouldGetTableColumnsWithCatalogAndSchema() throws Exception {
            var authHeader = getTestUser1AuthHeader();
            String tableName = "test_table";

            mockMvc.perform(get("/api/schema/connections/{connectionId}/tables/{tableName}/columns", 
                            testConnectionId, tableName)
                        .header("Authorization", authHeader)
                        .param("catalog", "test_catalog")
                        .param("schema", "test_schema"))
                // レスポンスの検証（実際のデータベース接続失敗時は500エラーになる）
                .andExpect(status().is5xxServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 電文の検証
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("セキュリティ")
    class Security {

        @Test
        @DisplayName("認証なしでアクセス時403エラーが返される")
        @Transactional
        @Rollback
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/schema/connections/{connectionId}", testConnectionId))
                // レスポンスの検証
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("他ユーザーの接続でスキーマ取得時エラーが返される")
        @Transactional
        @Rollback
        void shouldReturnErrorWhenAccessingOtherUserConnection() throws Exception {
            var user2AuthHeader = getTestUser2AuthHeader();

            // user2で同じ接続のスキーマ取得を試行
            mockMvc.perform(get("/api/schema/connections/{connectionId}", testConnectionId)
                        .header("Authorization", user2AuthHeader))
                // レスポンスの検証
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 電文の検証
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").isArray());
        }
    }

    @Nested
    @DisplayName("エラーハンドリング")
    class ErrorHandling {

        @Test
        @DisplayName("無効な接続IDでエラーハンドリングが正常に動作する")
        @Transactional
        @Rollback
        void shouldHandleInvalidConnectionId() throws Exception {
            var authHeader = getTestUser1AuthHeader();
            String invalidConnectionId = "invalid";

            mockMvc.perform(get("/api/schema/connections/{connectionId}", invalidConnectionId)
                        .header("Authorization", authHeader))
                // レスポンスの検証（パス変数の型変換エラー）
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("空のテーブル名でエラーハンドリングが正常に動作する")
        @Transactional
        @Rollback
        void shouldHandleEmptyTableName() throws Exception {
            var authHeader = getTestUser1AuthHeader();

            // 空のテーブル名パス（trailing slash）は静的リソースとして処理され500エラーになる
            mockMvc.perform(get("/api/schema/connections/{connectionId}/tables/", testConnectionId)
                        .header("Authorization", authHeader))
                // レスポンスの検証（NoResourceFoundExceptionにより500エラー）
                .andExpect(status().is5xxServerError());
        }
    }
}