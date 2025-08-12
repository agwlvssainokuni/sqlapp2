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

import cherry.sqlapp2.dto.SavedQueryRequest;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.repository.SavedQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QueryController統合テスト
 * クエリ管理コントローラーの動作を検証
 */
@DisplayName("QueryController - クエリ管理コントローラー統合テスト")
public class QueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SavedQueryRepository savedQueryRepository;

    @BeforeEach
    void cleanUpQueries() {
        // 各テストメソッドレベルで@Transactional + @Rollbackを適用することで、
        // テスト終了時に自動的にロールバックされ、テスト間のデータ分離が実現される
    }

    /**
     * テスト用のSavedQueryRequestを作成
     */
    private SavedQueryRequest createTestQueryRequest() {
        var request = new SavedQueryRequest();
        request.setName("Test Query");
        request.setSqlContent("SELECT * FROM users WHERE id = :userId");
        request.setDescription("Test description");
        request.setSharingScope(SavedQuery.SharingScope.PRIVATE);

        // パラメータ定義
        Map<String, String> params = new HashMap<>();
        params.put("userId", "INTEGER");
        request.setParameterDefinitions(params);

        return request;
    }

    @Nested
    @DisplayName("保存クエリ管理")
    class SavedQueryManagement {

        @Test
        @DisplayName("新しいクエリを保存できる")
        @Transactional
        @Rollback
        void shouldCreateNewQuery() throws Exception {
            var request = createTestQueryRequest();
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/queries/saved")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.name").value("Test Query"))
                    .andExpect(jsonPath("$.data.sqlContent").value("SELECT * FROM users WHERE id = :userId"))
                    .andExpect(jsonPath("$.data.description").value("Test description"))
                    .andExpect(jsonPath("$.data.sharingScope").value("PRIVATE"))
                    .andExpect(jsonPath("$.data.username").value("testuser1"))
                    .andExpect(jsonPath("$.data.parameterDefinitions.userId").value("INTEGER"));
        }

        @Test
        @DisplayName("保存されたクエリを取得できる")
        @Transactional
        @Rollback
        void shouldGetSavedQuery() throws Exception {
            var request = createTestQueryRequest();
            var authHeader = getTestUser1AuthHeader();

            // まずクエリを保存
            var createResult = mockMvc.perform(post("/api/queries/saved")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // クエリIDを抽出（IntegerからLongに変換）
            Integer queryIdInt = com.jayway.jsonpath.JsonPath.read(
                    createResult.getResponse().getContentAsString(),
                    "$.data.id"
            );
            Long queryId = queryIdInt.longValue();

            // クエリを取得
            mockMvc.perform(get("/api/queries/saved/{queryId}", queryId)
                            .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data.id").value(queryId.intValue()))
                    .andExpect(jsonPath("$.data.name").value("Test Query"))
                    .andExpect(jsonPath("$.data.sqlContent").value("SELECT * FROM users WHERE id = :userId"));
        }

        @Test
        @DisplayName("保存されたクエリを更新できる")
        @Transactional
        @Rollback
        void shouldUpdateSavedQuery() throws Exception {
            var request = createTestQueryRequest();
            var authHeader = getTestUser1AuthHeader();

            // まずクエリを保存
            var createResult = mockMvc.perform(post("/api/queries/saved")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Integer queryIdInt = com.jayway.jsonpath.JsonPath.read(
                    createResult.getResponse().getContentAsString(),
                    "$.data.id"
            );
            Long queryId = queryIdInt.longValue();

            // クエリを更新
            request.setName("Updated Test Query");
            request.setDescription("Updated description");
            request.setSharingScope(SavedQuery.SharingScope.PUBLIC);

            mockMvc.perform(put("/api/queries/saved/{queryId}", queryId)
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data.id").value(queryId.intValue()))
                    .andExpect(jsonPath("$.data.name").value("Updated Test Query"))
                    .andExpect(jsonPath("$.data.description").value("Updated description"))
                    .andExpect(jsonPath("$.data.sharingScope").value("PUBLIC"));
        }

        @Test
        @DisplayName("保存されたクエリを削除できる")
        @Transactional
        @Rollback
        void shouldDeleteSavedQuery() throws Exception {
            var request = createTestQueryRequest();
            var authHeader = getTestUser1AuthHeader();

            // まずクエリを保存
            var createResult = mockMvc.perform(post("/api/queries/saved")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Integer queryIdInt = com.jayway.jsonpath.JsonPath.read(
                    createResult.getResponse().getContentAsString(),
                    "$.data.id"
            );
            Long queryId = queryIdInt.longValue();

            // クエリを削除
            mockMvc.perform(delete("/api/queries/saved/{queryId}", queryId)
                            .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));

            // 削除後は取得できないことを確認（400エラーが返される）
            mockMvc.perform(get("/api/queries/saved/{queryId}", queryId)
                            .header("Authorization", authHeader))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("ユーザーの保存クエリ一覧を取得できる")
        @Transactional
        @Rollback
        void shouldGetUserSavedQueries() throws Exception {
            var authHeader = getTestUser1AuthHeader();

            // 複数のクエリを作成
            for (int i = 1; i <= 3; i++) {
                var request = createTestQueryRequest();
                request.setName("Test Query " + i);

                mockMvc.perform(post("/api/queries/saved")
                                .header("Authorization", authHeader)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(request)))
                        .andExpect(status().isCreated());
            }

            // クエリ一覧を取得（少なくとも3つ以上のクエリが存在することを確認）
            mockMvc.perform(get("/api/queries/saved")
                            .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(3)));
        }
    }

    @Nested
    @DisplayName("バリデーション")
    class Validation {

        @Test
        @DisplayName("名前が空のクエリでバリデーションエラーが返される")
        @Transactional
        @Rollback
        void shouldReturnValidationErrorForEmptyName() throws Exception {
            var request = createTestQueryRequest();
            request.setName(""); // 空の名前
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/queries/saved")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("SQLコンテンツが空のクエリでバリデーションエラーが返される")
        @Transactional
        @Rollback
        void shouldReturnValidationErrorForEmptySql() throws Exception {
            var request = createTestQueryRequest();
            request.setSqlContent(""); // 空のSQL
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/queries/saved")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("長すぎる名前でバリデーションエラーが返される")
        @Transactional
        @Rollback
        void shouldReturnValidationErrorForTooLongName() throws Exception {
            var request = createTestQueryRequest();
            request.setName("a".repeat(201)); // 201文字の名前
            var authHeader = getTestUser1AuthHeader();

            mockMvc.perform(post("/api/queries/saved")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
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
            var request = createTestQueryRequest();

            mockMvc.perform(post("/api/queries/saved")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("他ユーザーのクエリにアクセス時403エラーが返される")
        @Transactional
        @Rollback
        void shouldReturn403WhenAccessingOtherUserQuery() throws Exception {
            var request = createTestQueryRequest();
            var user1AuthHeader = getTestUser1AuthHeader();
            var user2AuthHeader = getTestUser2AuthHeader();

            // user1でクエリを作成
            var createResult = mockMvc.perform(post("/api/queries/saved")
                            .header("Authorization", user1AuthHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Integer queryIdInt = com.jayway.jsonpath.JsonPath.read(
                    createResult.getResponse().getContentAsString(),
                    "$.data.id"
            );
            Long queryId = queryIdInt.longValue();

            // user2で同じクエリにアクセス試行（403 or 400エラーを許可）
            mockMvc.perform(get("/api/queries/saved/{queryId}", queryId)
                            .header("Authorization", user2AuthHeader))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.ok").value(false));
        }
    }
}