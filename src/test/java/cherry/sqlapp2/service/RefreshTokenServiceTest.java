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

import cherry.sqlapp2.entity.RefreshToken;
import cherry.sqlapp2.enums.Role;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.enums.UserStatus;
import cherry.sqlapp2.repository.RefreshTokenRepository;
import cherry.sqlapp2.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService - リフレッシュトークン管理")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    private RefreshTokenService refreshTokenService;

    private User testUser;
    private final String testUsername = "testUser";
    private final String testRefreshToken = "test.refresh.token";
    private final Long refreshTokenExpiration = 86400L; // 24 hours in seconds

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtUtil);
        
        testUser = new User(1L, testUsername, "hashedPassword", "test@example.com", Role.USER, UserStatus.APPROVED, "en");
    }

    @Nested
    @DisplayName("リフレッシュトークン作成")
    class RefreshTokenCreation {

        @Test
        @DisplayName("ユーザーに対して新しいリフレッシュトークンを作成する")
        void shouldCreateNewRefreshTokenForUser() {
            // Given
            when(jwtUtil.generateRefreshToken(testUsername, Role.USER)).thenReturn(testRefreshToken);
            when(jwtUtil.getRefreshTokenExpiration()).thenReturn(refreshTokenExpiration);
            
            RefreshToken mockSavedToken = new RefreshToken(testRefreshToken, testUser, 
                LocalDateTime.now().plusSeconds(refreshTokenExpiration));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mockSavedToken);

            // When
            RefreshToken result = refreshTokenService.createRefreshToken(testUser);

            // Then
            assertThat(result).isNotNull();
            verify(jwtUtil).generateRefreshToken(testUsername, Role.USER);
            verify(jwtUtil).getRefreshTokenExpiration();
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("作成されたトークンが正しい有効期限を持つ")
        void shouldCreateTokenWithCorrectExpiration() {
            // Given
            when(jwtUtil.generateRefreshToken(testUsername, Role.USER)).thenReturn(testRefreshToken);
            when(jwtUtil.getRefreshTokenExpiration()).thenReturn(refreshTokenExpiration);
            
            RefreshToken savedToken = new RefreshToken(testRefreshToken, testUser, 
                LocalDateTime.now().plusSeconds(refreshTokenExpiration));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // When
            RefreshToken result = refreshTokenService.createRefreshToken(testUser);

            // Then
            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusSeconds(refreshTokenExpiration + 10));
        }
    }

    @Nested
    @DisplayName("リフレッシュトークン検証")
    class RefreshTokenValidation {

        @Test
        @DisplayName("有効なリフレッシュトークンを検証する")
        void shouldValidateValidRefreshToken() {
            // Given
            String tokenValue = "valid.refresh.token";
            RefreshToken validToken = new RefreshToken(tokenValue, testUser, 
                LocalDateTime.now().plusDays(1));
            
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(validToken));

            // When
            Optional<RefreshToken> result = refreshTokenService.validateRefreshToken(tokenValue);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(validToken);
        }

        @Test
        @DisplayName("期限切れのリフレッシュトークンを拒否する")
        void shouldRejectExpiredRefreshToken() {
            // Given
            String tokenValue = "expired.refresh.token";
            RefreshToken expiredToken = new RefreshToken(tokenValue, testUser, 
                LocalDateTime.now().minusDays(1)); // Already expired
            
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(expiredToken));

            // When
            Optional<RefreshToken> result = refreshTokenService.validateRefreshToken(tokenValue);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("無効化されたリフレッシュトークンを拒否する")
        void shouldRejectRevokedRefreshToken() {
            // Given
            String tokenValue = "revoked.refresh.token";
            RefreshToken revokedToken = new RefreshToken(tokenValue, testUser, 
                LocalDateTime.now().plusDays(1));
            revokedToken.revoke(); // Mark as revoked
            
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(revokedToken));

            // When
            Optional<RefreshToken> result = refreshTokenService.validateRefreshToken(tokenValue);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("存在しないリフレッシュトークンを拒否する")
        void shouldRejectNonExistentRefreshToken() {
            // Given
            String tokenValue = "non.existent.token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // When
            Optional<RefreshToken> result = refreshTokenService.validateRefreshToken(tokenValue);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("リフレッシュトークン使用")
    class RefreshTokenUsage {

        @Test
        @DisplayName("スライディング有効期限が無効の場合、有効期限を延長しない")
        void shouldNotExtendExpirationWhenSlidingIsDisabled() {
            // Given
            String tokenValue = "sliding.disabled.token";
            LocalDateTime originalExpiration = LocalDateTime.now().plusDays(1);
            RefreshToken token = new RefreshToken(tokenValue, testUser, originalExpiration);
            
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));
            when(jwtUtil.isSlidingRefreshExpiration()).thenReturn(false);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);

            // When
            Optional<RefreshToken> result = refreshTokenService.useRefreshToken(tokenValue);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getExpiresAt()).isEqualTo(originalExpiration);
            assertThat(result.get().getLastUsedAt()).isNotNull();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("スライディング有効期限が有効の場合、有効期限を延長する")
        void shouldExtendExpirationWhenSlidingIsEnabled() {
            // Given
            String tokenValue = "sliding.enabled.token";
            LocalDateTime originalExpiration = LocalDateTime.now().plusDays(1);
            RefreshToken token = new RefreshToken(tokenValue, testUser, originalExpiration);
            
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));
            when(jwtUtil.isSlidingRefreshExpiration()).thenReturn(true);
            when(jwtUtil.getRefreshTokenExpiration()).thenReturn(refreshTokenExpiration);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);

            // When
            Optional<RefreshToken> result = refreshTokenService.useRefreshToken(tokenValue);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getExpiresAt()).isAfter(originalExpiration);
            assertThat(result.get().getLastUsedAt()).isNotNull();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("無効なトークンを使用しようとすると空のOptionalを返す")
        void shouldReturnEmptyOptionalForInvalidToken() {
            // Given
            String tokenValue = "invalid.token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // When
            Optional<RefreshToken> result = refreshTokenService.useRefreshToken(tokenValue);

            // Then
            assertThat(result).isEmpty();
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("リフレッシュトークン無効化")
    class RefreshTokenRevocation {

        @Test
        @DisplayName("特定のリフレッシュトークンを無効化する")
        void shouldRevokeSpecificRefreshToken() {
            // Given
            RefreshToken token = new RefreshToken(testRefreshToken, testUser, 
                LocalDateTime.now().plusDays(1));

            // When
            refreshTokenService.revokeToken(token);

            // Then
            assertThat(token.getIsRevoked()).isTrue();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("トークン値で特定のリフレッシュトークンを無効化する")
        void shouldRevokeTokenByValue() {
            // Given
            String tokenValue = "token.to.revoke";
            RefreshToken token = new RefreshToken(tokenValue, testUser, 
                LocalDateTime.now().plusDays(1));
            
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

            // When
            boolean result = refreshTokenService.revokeToken(tokenValue);

            // Then
            assertThat(result).isTrue();
            assertThat(token.getIsRevoked()).isTrue();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("存在しないトークンの無効化はfalseを返す")
        void shouldReturnFalseForNonExistentToken() {
            // Given
            String tokenValue = "non.existent.token";
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // When
            boolean result = refreshTokenService.revokeToken(tokenValue);

            // Then
            assertThat(result).isFalse();
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("ユーザーの全リフレッシュトークンを無効化する")
        void shouldRevokeAllTokensForUser() {
            // When
            refreshTokenService.revokeAllUserTokens(testUser);

            // Then
            verify(refreshTokenRepository).revokeAllTokensByUser(testUser);
        }
    }

    @Nested
    @DisplayName("ユーザー権限チェック")
    class UserPermissionChecks {

        @Test
        @DisplayName("トークン所有者を正しく検証する")
        void shouldValidateTokenOwnership() {
            // Given
            RefreshToken token = new RefreshToken(testRefreshToken, testUser, 
                LocalDateTime.now().plusDays(1));

            // When
            boolean isOwner = refreshTokenService.isTokenOwner(token, testUser);

            // Then
            assertThat(isOwner).isTrue();
        }

        @Test
        @DisplayName("トークン所有者でない場合はfalseを返す")
        void shouldReturnFalseForNonOwner() {
            // Given
            User otherUser = new User(2L, "otherUser", "password", "other@example.com", Role.USER, UserStatus.APPROVED, "en");
            RefreshToken token = new RefreshToken(testRefreshToken, testUser, 
                LocalDateTime.now().plusDays(1));

            // When
            boolean isOwner = refreshTokenService.isTokenOwner(token, otherUser);

            // Then
            assertThat(isOwner).isFalse();
        }

        @Test
        @DisplayName("ユーザーがトークンをリフレッシュできることを確認する")
        void shouldAllowTokenRefreshForUser() {
            // When
            boolean canRefresh = refreshTokenService.canRefreshToken(testUser);

            // Then
            assertThat(canRefresh).isTrue();
        }
    }

    @Nested
    @DisplayName("クリーンアップ処理")
    class CleanupOperations {

        @Test
        @DisplayName("期限切れトークンをクリーンアップする")
        void shouldCleanupExpiredTokens() {
            // When
            refreshTokenService.cleanupExpiredTokens();

            // Then
            verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("無効化されたトークンをクリーンアップする")
        void shouldCleanupRevokedTokens() {
            // When
            refreshTokenService.cleanupRevokedTokens();

            // Then
            verify(refreshTokenRepository).deleteRevokedTokens();
        }
    }
}