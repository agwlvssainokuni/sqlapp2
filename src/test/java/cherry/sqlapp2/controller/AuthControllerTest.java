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
package cherry.sqlapp2.controller;

import cherry.sqlapp2.dto.*;
import cherry.sqlapp2.entity.Role;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.entity.UserStatus;
import cherry.sqlapp2.service.MetricsService;
import cherry.sqlapp2.service.RefreshTokenService;
import cherry.sqlapp2.service.UserService;
import cherry.sqlapp2.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - 認証API")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private MetricsService metricsService;

    private AuthController authController;

    private final String testUsername = "testUser";
    private final String testPassword = "testPassword123";
    private final String testEmail = "test@example.com";
    private final String testAccessToken = "test.access.token";
    private final String testRefreshToken = "test.refresh.token";
    private final Long testAccessExpirationTime = 300L;
    private final Long testRefreshExpirationTime = 86400L;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                userService,
                authenticationManager,
                jwtUtil,
                refreshTokenService,
                metricsService
        );
    }

    @Nested
    @DisplayName("ログイン処理")
    class Login {

        @Test
        @DisplayName("有効な認証情報でログインが成功する")
        void shouldLoginSuccessfullyWithValidCredentials() {
            // Given
            LoginRequest loginRequest = new LoginRequest(testUsername, testPassword);
            User user = new User(testUsername, "hashedPassword", testEmail);
            user.setId(1L);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            Authentication mockAuth = mock(Authentication.class);
            cherry.sqlapp2.entity.RefreshToken refreshToken = new cherry.sqlapp2.entity.RefreshToken(
                    testRefreshToken, user, java.time.LocalDateTime.now().plusDays(1)
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(userService.findByUsername(testUsername)).thenReturn(Optional.of(user));
            when(jwtUtil.generateAccessToken(testUsername, Role.USER)).thenReturn(testAccessToken);
            when(jwtUtil.getAccessTokenExpiration()).thenReturn(testAccessExpirationTime);
            when(jwtUtil.getRefreshTokenExpiration()).thenReturn(testRefreshExpirationTime);
            when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

            // When
            ApiResponse<LoginResult> response = authController.login(loginRequest);

            // Then
            assertThat(response.ok()).isTrue();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().accessToken()).isEqualTo(testAccessToken);
            assertThat(response.data().refreshToken()).isEqualTo(testRefreshToken);
            assertThat(response.data().expiresIn()).isEqualTo(testAccessExpirationTime);
            assertThat(response.data().refreshExpiresIn()).isEqualTo(testRefreshExpirationTime);
            assertThat(response.data().user().username()).isEqualTo(testUsername);
            assertThat(response.data().user().email()).isEqualTo(testEmail);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userService).findByUsername(testUsername);
            verify(jwtUtil).generateAccessToken(testUsername, Role.USER);
            verify(jwtUtil).getAccessTokenExpiration();
            verify(jwtUtil).getRefreshTokenExpiration();
            verify(refreshTokenService).createRefreshToken(user);
        }

        @Test
        @DisplayName("無効な認証情報でログインが失敗する")
        void shouldFailLoginWithInvalidCredentials() {
            // Given
            LoginRequest loginRequest = new LoginRequest(testUsername, "wrongPassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // When & Then
            assertThatThrownBy(() -> authController.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userService, never()).findByUsername(anyString());
            verify(jwtUtil, never()).generateAccessToken(anyString(), any(Role.class));
        }

        @Test
        @DisplayName("認証後にユーザーが見つからない場合の処理")
        void shouldHandleMissingUserAfterAuthentication() {
            // Given
            LoginRequest loginRequest = new LoginRequest(testUsername, testPassword);
            Authentication mockAuth = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(userService.findByUsername(testUsername)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authController.login(loginRequest))
                    .isInstanceOf(RuntimeException.class);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userService).findByUsername(testUsername);
            verify(jwtUtil, never()).generateAccessToken(anyString(), any(Role.class));
        }

        @Test
        @DisplayName("特殊文字を含むユーザー名での認証")
        void shouldHandleSpecialCharactersInUsername() {
            // Given
            String specialUsername = "user@domain.com+test!#$%";
            LoginRequest loginRequest = new LoginRequest(specialUsername, testPassword);
            User user = new User(specialUsername, "hashedPassword", testEmail);
            user.setId(2L);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            Authentication mockAuth = mock(Authentication.class);
            cherry.sqlapp2.entity.RefreshToken refreshToken = new cherry.sqlapp2.entity.RefreshToken(
                    testRefreshToken, user, java.time.LocalDateTime.now().plusDays(1)
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(userService.findByUsername(specialUsername)).thenReturn(Optional.of(user));
            when(jwtUtil.generateAccessToken(specialUsername, Role.USER)).thenReturn(testAccessToken);
            when(jwtUtil.getAccessTokenExpiration()).thenReturn(testAccessExpirationTime);
            when(jwtUtil.getRefreshTokenExpiration()).thenReturn(testRefreshExpirationTime);
            when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

            // When
            ApiResponse<LoginResult> response = authController.login(loginRequest);

            // Then
            assertThat(response.ok()).isTrue();
            assertThat(response.data().user().username()).isEqualTo(specialUsername);
        }
    }

    @Nested
    @DisplayName("ユーザー登録処理")
    class Registration {

        @Test
        @DisplayName("有効な情報でユーザー登録が成功する")
        void shouldRegisterUserSuccessfullyWithValidData() {
            // Given
            UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
                    testUsername, testPassword, testEmail
            );
            User createdUser = new User(testUsername, "hashedPassword", testEmail);

            when(userService.createUser(testUsername, testPassword, testEmail, "en"))
                    .thenReturn(createdUser);

            // When
            ResponseEntity<ApiResponse<LoginUser>> response = authController.register(registrationRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().ok()).isTrue();
            assertThat(response.getBody().data().username()).isEqualTo(testUsername);
            assertThat(response.getBody().data().email()).isEqualTo(testEmail);

            verify(userService).createUser(testUsername, testPassword, testEmail, "en");
        }

        @Test
        @DisplayName("重複するユーザー名で登録が失敗する")
        void shouldFailRegistrationWithDuplicateUsername() {
            // Given
            UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
                    testUsername, testPassword, testEmail
            );

            when(userService.createUser(testUsername, testPassword, testEmail, "en"))
                    .thenThrow(new IllegalArgumentException("Username already exists"));

            // When & Then
            assertThatThrownBy(() -> authController.register(registrationRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Username already exists");

            verify(userService).createUser(testUsername, testPassword, testEmail, "en");
        }

        @Test
        @DisplayName("重複するメールアドレスで登録が失敗する")
        void shouldFailRegistrationWithDuplicateEmail() {
            // Given
            UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
                    testUsername, testPassword, testEmail
            );

            when(userService.createUser(testUsername, testPassword, testEmail, "en"))
                    .thenThrow(new IllegalArgumentException("Email already exists"));

            // When & Then
            assertThatThrownBy(() -> authController.register(registrationRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists");

            verify(userService).createUser(testUsername, testPassword, testEmail, "en");
        }

        @Test
        @DisplayName("長いユーザー名での登録")
        void shouldHandleLongUsernameInRegistration() {
            // Given
            String longUsername = "a".repeat(100);
            UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
                    longUsername, testPassword, testEmail
            );
            User createdUser = new User(longUsername, "hashedPassword", testEmail);

            when(userService.createUser(longUsername, testPassword, testEmail, "en"))
                    .thenReturn(createdUser);

            // When
            ResponseEntity<ApiResponse<LoginUser>> response = authController.register(registrationRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().data().username()).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Unicode文字を含むデータでの登録")
        void shouldHandleUnicodeCharactersInRegistration() {
            // Given
            String unicodeUsername = "ユーザー名テスト123";
            String unicodeEmail = "テスト@example.com";
            UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
                    unicodeUsername, testPassword, unicodeEmail
            );
            User createdUser = new User(unicodeUsername, "hashedPassword", unicodeEmail);

            when(userService.createUser(unicodeUsername, testPassword, unicodeEmail, "en"))
                    .thenReturn(createdUser);

            // When
            ResponseEntity<ApiResponse<LoginUser>> response = authController.register(registrationRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().data().username()).isEqualTo(unicodeUsername);
            assertThat(response.getBody().data().email()).isEqualTo(unicodeEmail);
        }
    }

    @Nested
    @DisplayName("リフレッシュトークン処理")
    class RefreshTokenHandling {

        @Test
        @DisplayName("有効なリフレッシュトークンで新しいアクセストークンを取得する")
        void shouldRefreshAccessTokenWithValidRefreshToken() {
            // Given
            User user = new User(testUsername, "hashedPassword", testEmail);
            user.setId(1L);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);

            cherry.sqlapp2.dto.RefreshTokenRequest refreshRequest =
                    new cherry.sqlapp2.dto.RefreshTokenRequest(testRefreshToken);

            cherry.sqlapp2.entity.RefreshToken refreshTokenEntity =
                    new cherry.sqlapp2.entity.RefreshToken(testRefreshToken, user,
                            java.time.LocalDateTime.now().plusDays(1));

            cherry.sqlapp2.entity.RefreshToken newRefreshTokenEntity =
                    new cherry.sqlapp2.entity.RefreshToken("new.refresh.token", user,
                            java.time.LocalDateTime.now().plusDays(1));

            String newAccessToken = "new.access.token";

            when(refreshTokenService.useRefreshToken(testRefreshToken))
                    .thenReturn(Optional.of(refreshTokenEntity));
            when(refreshTokenService.canRefreshToken(user)).thenReturn(true);
            when(jwtUtil.validateRefreshToken(testRefreshToken, testUsername)).thenReturn(true);
            when(jwtUtil.generateAccessToken(testUsername, Role.USER)).thenReturn(newAccessToken);
            when(jwtUtil.getAccessTokenExpiration()).thenReturn(testAccessExpirationTime);
            when(jwtUtil.getRefreshTokenExpiration()).thenReturn(testRefreshExpirationTime);
            when(jwtUtil.isSlidingRefreshExpiration()).thenReturn(false);
            when(refreshTokenService.createRefreshToken(user)).thenReturn(newRefreshTokenEntity);

            // When
            cherry.sqlapp2.dto.ApiResponse<cherry.sqlapp2.dto.RefreshTokenResult> response =
                    authController.refresh(refreshRequest);

            // Then
            assertThat(response.ok()).isTrue();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().accessToken()).isEqualTo(newAccessToken);
            assertThat(response.data().refreshToken()).isEqualTo("new.refresh.token");
            assertThat(response.data().expiresIn()).isEqualTo(testAccessExpirationTime);
            assertThat(response.data().refreshExpiresIn()).isEqualTo(testRefreshExpirationTime);

            verify(refreshTokenService).useRefreshToken(testRefreshToken);
            verify(refreshTokenService).canRefreshToken(user);
            verify(jwtUtil).validateRefreshToken(testRefreshToken, testUsername);
            verify(jwtUtil).generateAccessToken(testUsername, Role.USER);
            verify(refreshTokenService).createRefreshToken(user);
            verify(refreshTokenService).revokeToken(refreshTokenEntity);
        }

        @Test
        @DisplayName("無効なリフレッシュトークンでエラーを返す")
        void shouldReturnErrorForInvalidRefreshToken() {
            // Given
            cherry.sqlapp2.dto.RefreshTokenRequest refreshRequest =
                    new cherry.sqlapp2.dto.RefreshTokenRequest("invalid.refresh.token");

            when(refreshTokenService.useRefreshToken("invalid.refresh.token"))
                    .thenReturn(Optional.empty());

            // When
            cherry.sqlapp2.dto.ApiResponse<cherry.sqlapp2.dto.RefreshTokenResult> response =
                    authController.refresh(refreshRequest);

            // Then
            assertThat(response.ok()).isFalse();
            assertThat(response.error()).contains("Invalid or expired refresh token");

            verify(refreshTokenService).useRefreshToken("invalid.refresh.token");
            verify(refreshTokenService, never()).canRefreshToken(any());
        }

        @Test
        @DisplayName("リフレッシュが許可されていないユーザーでエラーを返す")
        void shouldReturnErrorWhenRefreshNotAllowedForUser() {
            // Given
            User user = new User(testUsername, "hashedPassword", testEmail);
            user.setId(1L);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.PENDING);

            cherry.sqlapp2.dto.RefreshTokenRequest refreshRequest =
                    new cherry.sqlapp2.dto.RefreshTokenRequest(testRefreshToken);

            cherry.sqlapp2.entity.RefreshToken refreshTokenEntity =
                    new cherry.sqlapp2.entity.RefreshToken(testRefreshToken, user,
                            java.time.LocalDateTime.now().plusDays(1));

            when(refreshTokenService.useRefreshToken(testRefreshToken))
                    .thenReturn(Optional.of(refreshTokenEntity));
            when(refreshTokenService.canRefreshToken(user)).thenReturn(false);

            // When
            cherry.sqlapp2.dto.ApiResponse<cherry.sqlapp2.dto.RefreshTokenResult> response =
                    authController.refresh(refreshRequest);

            // Then
            assertThat(response.ok()).isFalse();
            assertThat(response.error()).contains("Token refresh not allowed for this user");

            verify(refreshTokenService).useRefreshToken(testRefreshToken);
            verify(refreshTokenService).canRefreshToken(user);
        }
    }

    @Nested
    @DisplayName("ログアウト処理")
    class LogoutHandling {

        @Test
        @DisplayName("有効なリフレッシュトークンでログアウトが成功する")
        void shouldLogoutSuccessfullyWithValidRefreshToken() {
            // Given
            cherry.sqlapp2.dto.RefreshTokenRequest logoutRequest =
                    new cherry.sqlapp2.dto.RefreshTokenRequest(testRefreshToken);

            when(refreshTokenService.revokeToken(testRefreshToken)).thenReturn(true);

            // When
            cherry.sqlapp2.dto.ApiResponse<Void> response = authController.logout(logoutRequest);

            // Then
            assertThat(response.ok()).isTrue();
            verify(refreshTokenService).revokeToken(testRefreshToken);
        }

        @Test
        @DisplayName("無効なリフレッシュトークンでログアウトがエラーを返す")
        void shouldReturnErrorForInvalidRefreshTokenOnLogout() {
            // Given
            cherry.sqlapp2.dto.RefreshTokenRequest logoutRequest =
                    new cherry.sqlapp2.dto.RefreshTokenRequest("invalid.token");

            when(refreshTokenService.revokeToken("invalid.token")).thenReturn(false);

            // When
            cherry.sqlapp2.dto.ApiResponse<Void> response = authController.logout(logoutRequest);

            // Then
            assertThat(response.ok()).isFalse();
            assertThat(response.error()).contains("Invalid refresh token");
            verify(refreshTokenService).revokeToken("invalid.token");
        }
    }

    @Nested
    @DisplayName("現在のユーザー情報取得")
    class CurrentUser {

        @Test
        @DisplayName("getCurrentUser メソッドはセキュリティコンテキストに依存するため、統合テストで検証")
        void shouldBeTestedInIntegrationTests() {
            // このメソッドはSecurityContextHolderに依存しているため、
            // 単体テストでは適切にテストできません。
            // 統合テストで検証する必要があります。
            assertThat(authController).isNotNull();
        }
    }

    @Nested
    @DisplayName("セキュリティとエッジケース")
    class SecurityAndEdgeCases {

        @Test
        @DisplayName("認証マネージャーが null を返した場合の処理")
        void shouldHandleNullAuthenticationResult() {
            // Given
            LoginRequest loginRequest = new LoginRequest(testUsername, testPassword);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> authController.login(loginRequest))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("JWTトークン生成失敗時の処理")
        void shouldHandleJwtGenerationFailure() {
            // Given
            LoginRequest loginRequest = new LoginRequest(testUsername, testPassword);
            User user = new User(testUsername, "hashedPassword", testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            Authentication mockAuth = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(userService.findByUsername(testUsername)).thenReturn(Optional.of(user));
            when(jwtUtil.generateAccessToken(testUsername, Role.USER)).thenThrow(new RuntimeException("JWT generation failed"));

            // When & Then
            assertThatThrownBy(() -> authController.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JWT generation failed");
        }

        @Test
        @DisplayName("空文字列のパスワードでの認証試行")
        void shouldHandleEmptyPasswordAuthentication() {
            // Given
            LoginRequest loginRequest = new LoginRequest(testUsername, "");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Empty password"));

            // When & Then
            assertThatThrownBy(() -> authController.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Empty password");
        }

        @Test
        @DisplayName("null値を含むリクエストの処理")
        void shouldHandleNullValuesInRequests() {
            // Given
            LoginRequest loginRequest = new LoginRequest(null, null);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Null credentials"));

            // When & Then
            assertThatThrownBy(() -> authController.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Null credentials");
        }
    }
}