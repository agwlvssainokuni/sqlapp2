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

import cherry.sqlapp2.dto.LoginRequest;
import cherry.sqlapp2.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 統合テスト基底クラス
 * 共通的なテスト設定・ヘルパーメソッドを提供
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationtest")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Sql(scripts = "/data-integration-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class BaseIntegrationTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtUtil jwtUtil;

    protected MockMvc mockMvc;

    // テストユーザーの定数
    protected static final String TEST_USER_1 = "testuser1";
    protected static final String TEST_USER_2 = "testuser2";
    protected static final String TEST_USER_3 = "testuser3";
    protected static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // セキュリティコンテキストをクリア
        SecurityContextHolder.clearContext();
    }

    /**
     * テストユーザーでログインしてJWTトークンを取得
     *
     * @param username ユーザー名
     * @return JWTトークン
     * @throws Exception ログイン処理でエラーが発生した場合
     */
    protected String loginAndGetToken(String username) throws Exception {
        var loginRequest = new LoginRequest(username, TEST_PASSWORD);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // レスポンス確認
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 電文確認
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andReturn();

        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.access_token"
        );
    }

    /**
     * JWTトークンからAuthorizationヘッダー値を生成
     *
     * @param token JWTトークン
     * @return Authorizationヘッダー値（"Bearer {token}"形式）
     */
    protected String createAuthorizationHeader(String token) {
        return "Bearer " + token;
    }

    /**
     * テストユーザー1でログインしてAuthorizationヘッダー値を取得
     *
     * @return Authorizationヘッダー値
     * @throws Exception ログイン処理でエラーが発生した場合
     */
    protected String getTestUser1AuthHeader() throws Exception {
        String token = loginAndGetToken(TEST_USER_1);
        return createAuthorizationHeader(token);
    }

    /**
     * テストユーザー2でログインしてAuthorizationヘッダー値を取得
     *
     * @return Authorizationヘッダー値
     * @throws Exception ログイン処理でエラーが発生した場合
     */
    protected String getTestUser2AuthHeader() throws Exception {
        String token = loginAndGetToken(TEST_USER_2);
        return createAuthorizationHeader(token);
    }

    /**
     * テストユーザー3でログインしてAuthorizationヘッダー値を取得
     *
     * @return Authorizationヘッダー値
     * @throws Exception ログイン処理でエラーが発生した場合
     */
    protected String getTestUser3AuthHeader() throws Exception {
        String token = loginAndGetToken(TEST_USER_3);
        return createAuthorizationHeader(token);
    }

    /**
     * JSONレスポンスをオブジェクトに変換
     *
     * @param <T>   変換先の型
     * @param json  JSON文字列
     * @param clazz 変換先のクラス
     * @return 変換されたオブジェクト
     * @throws Exception JSON解析エラーが発生した場合
     */
    protected <T> T parseJsonResponse(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }

    /**
     * JSONレスポンスをオブジェクトに変換（TypeReference版）
     *
     * @param <T>     変換先の型
     * @param json    JSON文字列
     * @param typeRef TypeReference
     * @return 変換されたオブジェクト
     * @throws Exception JSON解析エラーが発生した場合
     */
    protected <T> T parseJsonResponse(String json, com.fasterxml.jackson.core.type.TypeReference<T> typeRef) throws Exception {
        return objectMapper.readValue(json, typeRef);
    }

    /**
     * オブジェクトをJSON文字列に変換
     *
     * @param object 変換対象のオブジェクト
     * @return JSON文字列
     * @throws Exception JSON生成エラーが発生した場合
     */
    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
}
