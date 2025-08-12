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
package cherry.sqlapp2.security;

import cherry.sqlapp2.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter - セキュリティフィルター")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String testUsername = "testUser";
    private final String validToken = "valid.jwt.token";
    private final String invalidToken = "invalid.jwt.token";

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "userDetailsService", userDetailsService);
        
        // SecurityContextHolderの初期化
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("有効なJWTトークン処理")
    class ValidTokenProcessing {

        @Test
        @DisplayName("有効なBearerトークンで認証が成功する")
        void shouldAuthenticateWithValidBearerToken() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + validToken;
            UserDetails userDetails = new User(testUsername, "password", 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(userDetails);
            when(jwtUtil.validateAccessToken(validToken, testUsername)).thenReturn(true);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtUtil).extractUsername(validToken);
            verify(userDetailsService).loadUserByUsername(testUsername);
            verify(jwtUtil).validateAccessToken(validToken, testUsername);
            verify(securityContext).setAuthentication(any(Authentication.class));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("有効なトークンでユーザー詳細が取得される")
        void shouldLoadUserDetailsWithValidToken() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + validToken;
            UserDetails userDetails = new User(testUsername, "password", 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(userDetails);
            when(jwtUtil.validateAccessToken(validToken, testUsername)).thenReturn(true);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(userDetailsService).loadUserByUsername(testUsername);
            verify(securityContext).setAuthentication(argThat(auth -> 
                auth.getPrincipal().equals(userDetails) &&
                auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
            ));
        }

        @Test
        @DisplayName("複数の権限を持つユーザーで認証が成功する")
        void shouldAuthenticateUserWithMultipleAuthorities() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + validToken;
            UserDetails userDetails = new User(testUsername, "password", 
                    java.util.List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                    ));

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(userDetails);
            when(jwtUtil.validateAccessToken(validToken, testUsername)).thenReturn(true);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(securityContext).setAuthentication(argThat(auth -> 
                auth.getAuthorities().size() == 2 &&
                auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")) &&
                auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
            ));
        }
    }

    @Nested
    @DisplayName("無効なJWTトークン処理")
    class InvalidTokenProcessing {

        @Test
        @DisplayName("無効なトークンで認証が失敗する")
        void shouldFailAuthenticationWithInvalidToken() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + invalidToken;

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(invalidToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(
                    new User(testUsername, "password", Collections.emptyList()));
            when(jwtUtil.validateAccessToken(invalidToken, testUsername)).thenReturn(false);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtUtil).validateAccessToken(invalidToken, testUsername);
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("トークン抽出で例外が発生した場合の処理")
        void shouldHandleTokenExtractionException() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + invalidToken;

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(invalidToken)).thenThrow(new RuntimeException("Invalid token format"));

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtUtil).extractUsername(invalidToken);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("ユーザーが見つからない場合の処理")
        void shouldHandleUserNotFound() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + validToken;

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(testUsername))
                    .thenThrow(new UsernameNotFoundException("User not found"));

            // When & Then
            assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");

            verify(userDetailsService).loadUserByUsername(testUsername);
            verify(jwtUtil, never()).validateAccessToken(anyString(), anyString());
            verify(securityContext, never()).setAuthentication(any());
        }

        @Test
        @DisplayName("期限切れトークンで認証が失敗する")
        void shouldFailWithExpiredToken() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer expired.token.here";

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername("expired.token.here"))
                    .thenThrow(new RuntimeException("Token expired"));

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("認証ヘッダー処理")
    class AuthorizationHeaderProcessing {

        @Test
        @DisplayName("Authorizationヘッダーがない場合はスキップする")
        void shouldSkipWhenNoAuthorizationHeader() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtUtil, never()).extractUsername(anyString());
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer以外のヘッダーはスキップする")
        void shouldSkipNonBearerHeaders() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn("Basic dGVzdDp0ZXN0");

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtUtil, never()).extractUsername(anyString());
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("空のBearerトークンを処理する")
        void shouldHandleEmptyBearerToken() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn("Bearer ");

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtUtil).extractUsername("");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearerの大文字小文字を区別する")
        void shouldBeCaseSensitiveForBearer() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn("bearer " + validToken);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtUtil, never()).extractUsername(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("非常に長いトークンを処理する")
        void shouldHandleVeryLongToken() throws ServletException, IOException {
            // Given
            String longToken = "a".repeat(2000);
            String bearerToken = "Bearer " + longToken;

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(longToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(
                    new User(testUsername, "password", Collections.emptyList()));
            when(jwtUtil.validateAccessToken(longToken, testUsername)).thenReturn(true);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtUtil).extractUsername(longToken);
            verify(jwtUtil).validateAccessToken(longToken, testUsername);
        }
    }

    @Nested
    @DisplayName("セキュリティコンテキスト処理")
    class SecurityContextProcessing {

        @Test
        @DisplayName("既に認証されている場合はスキップする")
        void shouldSkipWhenAlreadyAuthenticated() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + validToken;
            Authentication existingAuth = mock(Authentication.class);

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(existingAuth);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(jwtUtil, never()).validateAccessToken(anyString(), anyString());
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("nullユーザー名の場合はスキップする")
        void shouldSkipWhenUsernameIsNull() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + validToken;

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(null);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(jwtUtil, never()).validateAccessToken(anyString(), anyString());
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("認証後のセキュリティコンテキストが正しく設定される")
        void shouldSetCorrectSecurityContext() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + validToken;
            UserDetails userDetails = new User(testUsername, "password", 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(userDetails);
            when(jwtUtil.validateAccessToken(validToken, testUsername)).thenReturn(true);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(securityContext).setAuthentication(any(Authentication.class));
        }
    }

    @Nested
    @DisplayName("エラーハンドリングとエッジケース")
    class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("フィルターチェーンが常に実行される")
        void shouldAlwaysExecuteFilterChain() throws ServletException, IOException {
            // Given - 様々なエラーケース
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
            when(jwtUtil.extractUsername("invalid"))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("UserDetailsService例外を適切に処理する")
        void shouldHandleUserDetailsServiceException() throws ServletException, IOException {
            // Given
            String bearerToken = "Bearer " + validToken;

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(testUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(testUsername))
                    .thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection error");

            verify(securityContext, never()).setAuthentication(any());
        }

        @Test
        @DisplayName("特殊文字を含むユーザー名を処理する")
        void shouldHandleSpecialCharactersInUsername() throws ServletException, IOException {
            // Given
            String specialUsername = "user@domain.com+test!#$%";
            String bearerToken = "Bearer " + validToken;
            UserDetails userDetails = new User(specialUsername, "password", Collections.emptyList());

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(specialUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(specialUsername)).thenReturn(userDetails);
            when(jwtUtil.validateAccessToken(validToken, specialUsername)).thenReturn(true);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(userDetailsService).loadUserByUsername(specialUsername);
            verify(jwtUtil).validateAccessToken(validToken, specialUsername);
            verify(securityContext).setAuthentication(any());
        }

        @Test
        @DisplayName("Unicode文字を含むユーザー名を処理する")
        void shouldHandleUnicodeCharactersInUsername() throws ServletException, IOException {
            // Given
            String unicodeUsername = "ユーザー名テスト123";
            String bearerToken = "Bearer " + validToken;
            UserDetails userDetails = new User(unicodeUsername, "password", Collections.emptyList());

            when(request.getHeader("Authorization")).thenReturn(bearerToken);
            when(jwtUtil.extractUsername(validToken)).thenReturn(unicodeUsername);
            when(securityContext.getAuthentication()).thenReturn(null);
            when(userDetailsService.loadUserByUsername(unicodeUsername)).thenReturn(userDetails);
            when(jwtUtil.validateAccessToken(validToken, unicodeUsername)).thenReturn(true);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(userDetailsService).loadUserByUsername(unicodeUsername);
            verify(securityContext).setAuthentication(any());
        }
    }
}