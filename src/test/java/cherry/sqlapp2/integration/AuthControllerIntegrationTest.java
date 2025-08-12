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

import cherry.sqlapp2.dto.ApiResponse;
import cherry.sqlapp2.dto.LoginRequest;
import cherry.sqlapp2.dto.LoginResult;
import cherry.sqlapp2.dto.UserRegistrationRequest;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController統合テスト
 * 認証機能の完全なフローテスト
 */
@DisplayName("AuthController - 認証コントローラー統合テスト")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("ユーザー登録")
    class UserRegistration {

        @Test
        @DisplayName("正常なユーザー登録ができる")
        void shouldSuccessfullyRegisterNewUser() throws Exception {
            // Given
            var registerRequest = new UserRegistrationRequest("newuser", "newpassword123", "newuser@example.com");

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // レスポンス確認
            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<LoginResult> response = parseJsonResponse(responseContent, new TypeReference<ApiResponse<LoginResult>>() {});
            
            assertThat(response.ok()).isTrue();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().user().username()).isEqualTo("newuser");
            assertThat(response.data().accessToken()).isNotEmpty();

            // データベース確認
            User savedUser = userRepository.findByUsername("newuser").orElse(null);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getUsername()).isEqualTo("newuser");
            assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
            assertThat(passwordEncoder.matches("newpassword123", savedUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("重複ユーザー名での登録は失敗する")
        void shouldFailToRegisterDuplicateUsername() throws Exception {
            // Given
            var registerRequest = new UserRegistrationRequest("testuser1", "password123", "duplicate@example.com");

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // レスポンス確認
            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<LoginResult> response = parseJsonResponse(responseContent, new TypeReference<ApiResponse<LoginResult>>() {});
            
            assertThat(response.ok()).isFalse();
            assertThat(response.error()).contains("Username already exists");
        }

        @Test
        @DisplayName("無効な入力での登録は失敗する")
        void shouldFailToRegisterWithInvalidInput() throws Exception {
            // Given - 空のユーザー名
            var registerRequest = new UserRegistrationRequest("", "password123", "invalid@example.com");

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("ユーザーログイン")
    class UserLogin {

        @Test
        @DisplayName("正しい認証情報でログインできる")
        void shouldSuccessfullyLoginWithCorrectCredentials() throws Exception {
            // Given
            var loginRequest = new LoginRequest("testuser1", "password123");

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // レスポンス確認
            String responseContent = result.getResponse().getContentAsString();
            LoginResult loginResult = parseJsonResponse(responseContent, LoginResult.class);
            
            assertThat(loginResult.user().username()).isEqualTo("testuser1");
            assertThat(loginResult.accessToken()).isNotEmpty();
            
            // JWTトークンの検証
            assertThat(jwtUtil.validateToken(loginResult.accessToken(), "testuser1")).isTrue();
            assertThat(jwtUtil.extractUsername(loginResult.accessToken())).isEqualTo("testuser1");
        }

        @Test
        @DisplayName("間違ったパスワードでログイン失敗する")
        void shouldFailToLoginWithWrongPassword() throws Exception {
            // Given
            var loginRequest = new LoginRequest("testuser1", "wrongpassword");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("存在しないユーザーでログイン失敗する")
        void shouldFailToLoginWithNonExistentUser() throws Exception {
            // Given
            var loginRequest = new LoginRequest("nonexistentuser", "password123");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("JWT認証統合")
    class JwtAuthenticationIntegration {

        @Test
        @DisplayName("登録からログインまでの完全フロー")
        void shouldCompleteRegistrationToLoginFlow() throws Exception {
            // Given
            String username = "flowtest";
            String password = "flowpassword123";
            String email = "flowtest@example.com";

            // Step 1: ユーザー登録
            var registerRequest = new UserRegistrationRequest(username, password, email);
            MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String registerContent = registerResult.getResponse().getContentAsString();
            ApiResponse<LoginResult> registerResponse = parseJsonResponse(registerContent, new TypeReference<ApiResponse<LoginResult>>() {});
            String registrationToken = registerResponse.data().accessToken();

            // Step 2: 登録時のトークンを検証
            assertThat(jwtUtil.validateToken(registrationToken, username)).isTrue();
            assertThat(jwtUtil.extractUsername(registrationToken)).isEqualTo(username);

            // Step 3: 再度ログイン
            var loginRequest = new LoginRequest(username, password);
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginContent = loginResult.getResponse().getContentAsString();
            LoginResult loginResponse = parseJsonResponse(loginContent, LoginResult.class);
            String loginToken = loginResponse.accessToken();

            // Step 4: ログイン時のトークンを検証
            assertThat(jwtUtil.validateToken(loginToken, username)).isTrue();
            assertThat(jwtUtil.extractUsername(loginToken)).isEqualTo(username);

            // Step 5: データベース状態確認
            User user = userRepository.findByUsername(username).orElse(null);
            assertThat(user).isNotNull();
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(passwordEncoder.matches(password, user.getPassword())).isTrue();
        }

        @Test
        @DisplayName("複数ユーザーの並行登録・ログイン")
        void shouldHandleMultipleUserRegistrationAndLogin() throws Exception {
            // Given
            String[] usernames = {"concurrent1", "concurrent2", "concurrent3"};
            String password = "concurrentpass123";

            // Step 1: 複数ユーザーを登録
            for (String username : usernames) {
                var registerRequest = new UserRegistrationRequest(username, password, username + "@example.com");
                mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                        .andExpect(status().isCreated());
            }

            // Step 2: 全ユーザーでログインし、各トークンを検証
            for (String username : usernames) {
                var loginRequest = new LoginRequest(username, password);
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                        .andExpect(status().isOk())
                        .andReturn();

                String responseContent = result.getResponse().getContentAsString();
                LoginResult loginResult = parseJsonResponse(responseContent, LoginResult.class);
                
                assertThat(loginResult.user().username()).isEqualTo(username);
                assertThat(jwtUtil.validateToken(loginResult.accessToken(), username)).isTrue();
                assertThat(jwtUtil.extractUsername(loginResult.accessToken())).isEqualTo(username);
            }

            // Step 3: データベースの状態確認
            for (String username : usernames) {
                User user = userRepository.findByUsername(username).orElse(null);
                assertThat(user).isNotNull();
                assertThat(user.getUsername()).isEqualTo(username);
            }
        }
    }
}