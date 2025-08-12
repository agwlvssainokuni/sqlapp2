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
import cherry.sqlapp2.repository.DatabaseConnectionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DatabaseConnectionController統合テスト
 * データベース接続管理コントローラーの動作を検証
 */
@DisplayName("DatabaseConnectionController - データベース接続管理統合テスト")
public class DatabaseConnectionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DatabaseConnectionRepository connectionRepository;

    @BeforeEach
    void beforeEach() {
        // トランザクションでテストの分離を行うため、手動クリーンアップは不要
    }

    /**
     * テスト用のDatabaseConnectionRequestを作成
     */
    private DatabaseConnectionRequest createTestConnectionRequest() {
        var request = new DatabaseConnectionRequest();
        request.setConnectionName("Test MySQL Connection");
        request.setDatabaseType(DatabaseType.MYSQL);
        request.setHost("localhost");
        request.setPort(3306);
        request.setDatabaseName("testdb");
        request.setUsername("testuser");
        request.setPassword("testpassword");
        request.setActive(true);
        return request;
    }

    @Nested
    @DisplayName("接続管理")
    class ConnectionManagement {

        @Test
        @DisplayName("新しいデータベース接続を作成できる")
        @Transactional
        @Rollback
        void shouldCreateNewConnection() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    // レスポンスの検証
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // 電文の検証
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.connectionName").value("Test MySQL Connection"))
                    .andExpect(jsonPath("$.data.databaseType").value("MYSQL"))
                    .andExpect(jsonPath("$.data.host").value("localhost"))
                    .andExpect(jsonPath("$.data.port").value(3306))
                    .andExpect(jsonPath("$.data.databaseName").value("testdb"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.additionalParams").isEmpty())
                    .andExpect(jsonPath("$.data.isActive").value(true))
                    .andExpect(jsonPath("$.data.createdAt").isString())
                    .andExpect(jsonPath("$.data.updatedAt").isString());
        }

        @Test
        @DisplayName("ユーザーのデータベース接続一覧を取得できる")
        @Transactional
        @Rollback
        void shouldGetUserConnections() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            Number beforeCount = JsonPath.read(
                    mockMvc.perform(get("/api/connections")
                                    .header("Authorization", authHeader))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.ok").value(true))
                            .andExpect(jsonPath("$.data").isArray())
                            .andReturn().getResponse().getContentAsString(),
                    "$.data.length()"
            );

            // 2つの接続を作成
            request.setConnectionName("Connection 1");
            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated());

            request.setConnectionName("Connection 2");
            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated());

            // 接続一覧を取得
            mockMvc.perform(get("/api/connections")
                            .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(
                            beforeCount.intValue() + 2
                    ));
        }

        @Test
        @DisplayName("アクティブな接続のみをフィルタできる")
        @Transactional
        @Rollback
        void shouldFilterActiveConnections() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            Number beforeCount = JsonPath.read(
                    mockMvc.perform(get("/api/connections?activeOnly=true")
                                    .header("Authorization", authHeader))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.ok").value(true))
                            .andExpect(jsonPath("$.data").isArray())
                            .andReturn().getResponse().getContentAsString(),
                    "$.data.length()"
            );

            // アクティブな接続を作成
            request.setConnectionName("Active Connection");
            request.setActive(true);
            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated());

            // 非アクティブな接続を作成
            request.setConnectionName("Inactive Connection");
            request.setActive(false);
            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated());

            // アクティブな接続のみを取得
            mockMvc.perform(get("/api/connections?activeOnly=true")
                            .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(
                            beforeCount.intValue() + 1
                    ))
                    .andExpect(jsonPath("$.data[*].active").value(
                            everyItem(is(true))
                    ));
        }

        @Test
        @DisplayName("データベース接続を更新できる")
        @Transactional
        @Rollback
        void shouldUpdateConnection() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            // 接続を作成
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
            Long connectionId = connectionIdInt.longValue();

            // 接続を更新
            request.setConnectionName("Updated MySQL Connection");
            request.setPort(3307);
            request.setDatabaseName("updateddb");
            request.setPassword(null); // パスワードは更新時null可能

            mockMvc.perform(put("/api/connections/{id}", connectionId)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    // レスポンスの検証
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // 電文の検証
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data.id").value(connectionId.intValue()))
                    .andExpect(jsonPath("$.data.connectionName").value("Updated MySQL Connection"))
                    .andExpect(jsonPath("$.data.port").value(3307))
                    .andExpect(jsonPath("$.data.databaseName").value("updateddb"));
        }

        @Test
        @DisplayName("データベース接続を削除できる")
        @Transactional
        @Rollback
        void shouldDeleteConnection() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            // 接続を作成
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
            Long connectionId = connectionIdInt.longValue();

            // 接続を削除
            mockMvc.perform(delete("/api/connections/{id}", connectionId)
                            .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }

    @Nested
    @DisplayName("接続テスト")
    class ConnectionTesting {

        @Test
        @DisplayName("データベース接続をテストできる")
        @Transactional
        @Rollback
        void shouldTestConnection() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            // 接続を作成
            request.setHost("dummy-host"); // 実際の接続は失敗する想定
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
            Long connectionId = connectionIdInt.longValue();

            // 接続テスト（実際の接続は失敗すると予想されるが、APIの動作を確認）
            mockMvc.perform(post("/api/connections/{id}/test", connectionId)
                            .header("Authorization", authHeader))
                    // レスポンスの検証
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // 電文の検証
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.success").isBoolean())
                    .andExpect(jsonPath("$.data.message").isString())
                    .andExpect(jsonPath("$.data.success").value(false)) // 接続は失敗する想定
                    .andExpect(jsonPath("$.data.message").value(
                            startsWith("Database connection failed: ")
                    ));
        }
    }

    @Nested
    @DisplayName("バリデーション")
    class Validation {

        @Test
        @DisplayName("接続名が空の場合バリデーションエラーが返される")
        @Transactional
        @Rollback
        void shouldReturnValidationErrorForEmptyConnectionName() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            request.setConnectionName(""); // 空の接続名

            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    // レスポンスの検証
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // 電文の検証
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.error").isArray())
                    .andExpect(jsonPath("$.error[*]").value(containsInAnyOrder(
                            "connectionName: Connection name is required",
                            "connectionName: Connection name must be between 1 and 100 characters"
                    )));
        }

        @Test
        @DisplayName("データベースタイプがnullの場合バリデーションエラーが返される")
        @Transactional
        @Rollback
        void shouldReturnValidationErrorForNullDatabaseType() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            request.setDatabaseType(null); // null データベースタイプ

            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    // レスポンスの検証
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // 電文の検証
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.error").isArray())
                    .andExpect(jsonPath("$.error[*]").value(containsInAnyOrder(
                            "databaseType: Database type is required"
                    )));
        }

        @Test
        @DisplayName("パスワードが空の新規作成でエラーが返される")
        @Transactional
        @Rollback
        void shouldReturnErrorForEmptyPasswordOnCreation() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            request.setPassword(""); // 空のパスワード

            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    // レスポンスの検証
                    .andExpect(status().isOk()) // ビジネスロジックエラーなので200
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // 電文の検証
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.error").isArray())
                    .andExpect(jsonPath("$.error[*]").value(containsInAnyOrder(
                            "Connection creation failed: Password is required"
                    )));
        }

        @Test
        @DisplayName("不正なポート番号でバリデーションエラーが返される")
        @Transactional
        @Rollback
        void shouldReturnValidationErrorForInvalidPort() throws Exception {
            var request = createTestConnectionRequest();
            var authHeader = getTestUser1AuthHeader();

            request.setPort(70000); // 範囲外のポート番号

            mockMvc.perform(post("/api/connections")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    // レスポンスの検証
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // 電文の検証
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.error").isArray())
                    .andExpect(jsonPath("$.error[*]").value(containsInAnyOrder(
                            "port: Port must be less than or equal to 65535"
                    )));
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
            var request = createTestConnectionRequest();

            mockMvc.perform(post("/api/connections")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    // レスポンスの検証
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("他ユーザーの接続にアクセス時エラーが返される")
        @Transactional
        @Rollback
        void shouldReturnErrorWhenAccessingOtherUserConnection() throws Exception {
            var request = createTestConnectionRequest();
            var user1AuthHeader = getTestUser1AuthHeader();
            var user2AuthHeader = getTestUser2AuthHeader();

            // user1で接続を作成
            var createResult = mockMvc.perform(post("/api/connections")
                            .header("Authorization", user1AuthHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Number connectionIdInt = JsonPath.read(
                    createResult.getResponse().getContentAsString(),
                    "$.data.id"
            );
            Long connectionId = connectionIdInt.longValue();

            // user2で同じ接続の削除を試行
            mockMvc.perform(delete("/api/connections/{id}", connectionId)
                            .header("Authorization", user2AuthHeader))
                    // レスポンスの検証
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // 電文の検証
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.error").isArray())
                    .andExpect(jsonPath("$.error[*]").value(containsInAnyOrder(
                            startsWith("Connection not found: ")
                    )));
        }
    }
}
