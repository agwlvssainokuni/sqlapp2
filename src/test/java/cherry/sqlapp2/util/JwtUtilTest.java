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
package cherry.sqlapp2.util;

import cherry.sqlapp2.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil - JWTトークン操作")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "ThisIsAVeryLongSecretKeyForTesting123456789012345678901234567890";
    private final Long testExpiration = 3600L; // 1時間 (in seconds)
    private final String testUsername = "testUser";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", testExpiration);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", testExpiration * 24); // 24 times longer
        ReflectionTestUtils.setField(jwtUtil, "slidingRefreshExpiration", false);
    }

    @Nested
    @DisplayName("トークン生成")
    class TokenGeneration {

        @Test
        @DisplayName("ユーザー名から有効なJWTトークンを生成する")
        void shouldGenerateValidTokenForUsername() {
            String token = jwtUtil.generateAccessToken(testUsername, Role.USER);

            assertThat(token).isNotNull()
                    .isNotBlank()
                    .contains(".");

            // トークンはドットで区切られた3つの部分を持つべき (header.payload.signature)
            String[] tokenParts = token.split("\\.");
            assertThat(tokenParts).hasSize(3);
        }

        @Test
        @DisplayName("同一ユーザー名でも異なる時刻で異なるトークンを生成する")
        void shouldGenerateDifferentTokensForSameUsername() throws InterruptedException {
            String token1 = jwtUtil.generateAccessToken(testUsername, Role.USER);
            Thread.sleep(1000); // 1秒待機して異なるタイムスタンプを確保
            String token2 = jwtUtil.generateAccessToken(testUsername, Role.USER);

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("異なるユーザー名で異なるトークンを生成する")
        void shouldGenerateDifferentTokensForDifferentUsernames() {
            String token1 = jwtUtil.generateAccessToken("user1", Role.USER);
            String token2 = jwtUtil.generateAccessToken("user2", Role.USER);

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("空のユーザー名を処理する")
        void shouldHandleEmptyUsername() {
            String token = jwtUtil.generateAccessToken("", Role.USER);

            assertThat(token).isNotNull().isNotBlank();
            // JJWTライブラリは空文字列のsubjectをJSONから省略するため、nullが返される
            assertThat(jwtUtil.extractUsername(token)).isNull();
        }

        @Test
        @DisplayName("nullユーザー名を処理する")
        void shouldHandleNullUsername() {
            String token = jwtUtil.generateAccessToken(null, Role.USER);

            assertThat(token).isNotNull();
            assertThat(jwtUtil.extractUsername(token)).isNull();
        }
    }

    @Nested
    @DisplayName("トークン解析とクレーム抽出")
    class TokenParsing {

        private String validToken;

        @BeforeEach
        void setUpToken() {
            validToken = jwtUtil.generateAccessToken(testUsername, Role.USER);
        }

        @Test
        @DisplayName("トークンから正しいユーザー名を抽出する")
        void shouldExtractCorrectUsername() {
            String extractedUsername = jwtUtil.extractUsername(validToken);

            assertThat(extractedUsername).isEqualTo(testUsername);
        }


        @Test
        @DisplayName("関数を使用してカスタムクレームを抽出する")
        void shouldExtractCustomClaims() {
            // 発行時刻を抽出
            Date issuedAt = jwtUtil.extractClaim(validToken, Claims::getIssuedAt);
            Date now = new Date();

            assertThat(issuedAt).isBefore(now);
            assertThat(now.getTime() - issuedAt.getTime()).isLessThan(1000); // 1秒以内
        }

        @Test
        @DisplayName("不正な形式のトークンで例外をスローする")
        void shouldThrowExceptionForMalformedToken() {
            String malformedToken = "invalid.token.format";

            assertThatThrownBy(() -> jwtUtil.extractUsername(malformedToken))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("無効な署名のトークンで例外をスローする")
        void shouldThrowExceptionForInvalidSignature() {
            // 異なる秘密鍵でトークンを作成
            JwtUtil differentKeyUtil = new JwtUtil();
            ReflectionTestUtils.setField(differentKeyUtil, "secret", "DifferentSecretKey123456789012345678901234567890123456");
            ReflectionTestUtils.setField(differentKeyUtil, "accessTokenExpiration", testExpiration);
            ReflectionTestUtils.setField(differentKeyUtil, "refreshTokenExpiration", testExpiration * 24);
            ReflectionTestUtils.setField(differentKeyUtil, "slidingRefreshExpiration", false);

            String tokenWithDifferentKey = differentKeyUtil.generateAccessToken(testUsername, Role.USER);

            assertThatThrownBy(() -> jwtUtil.extractUsername(tokenWithDifferentKey))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("nullトークンを処理する")
        void shouldHandleNullToken() {
            assertThatThrownBy(() -> jwtUtil.extractUsername(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空のトークンを処理する")
        void shouldHandleEmptyToken() {
            assertThatThrownBy(() -> jwtUtil.extractUsername(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("トークン検証")
    class TokenValidation {

        private String validToken;

        @BeforeEach
        void setUpToken() {
            validToken = jwtUtil.generateAccessToken(testUsername, Role.USER);
        }

        @Test
        @DisplayName("正しいトークンとユーザー名の組み合わせを検証する")
        void shouldValidateCorrectTokenAndUsername() {
            Boolean isValid = jwtUtil.validateAccessToken(validToken, testUsername);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("間違ったユーザー名のトークンを拒否する")
        void shouldRejectTokenWithWrongUsername() {
            Boolean isValid = jwtUtil.validateAccessToken(validToken, "wrongUsername");

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("期限切れトークンを拒否する")
        void shouldRejectExpiredToken() throws InterruptedException {
            // 非常に短い有効期限のutilを作成
            JwtUtil shortExpirationUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortExpirationUtil, "secret", testSecret);
            ReflectionTestUtils.setField(shortExpirationUtil, "accessTokenExpiration", 1L); // 1 second
            ReflectionTestUtils.setField(shortExpirationUtil, "refreshTokenExpiration", 1L);
            ReflectionTestUtils.setField(shortExpirationUtil, "slidingRefreshExpiration", false);

            String expiredToken = shortExpirationUtil.generateAccessToken(testUsername, Role.USER);

            // トークンの有効期限切れを待つ
            Thread.sleep(1500L); // 1.5秒待機

            Boolean isValid = jwtUtil.validateAccessToken(expiredToken, testUsername);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("不正な形式のトークンを拒否する")
        void shouldRejectMalformedToken() {
            Boolean isValid = jwtUtil.validateAccessToken("malformed.token", testUsername);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("nullトークンを拒否する")
        void shouldRejectNullToken() {
            Boolean isValid = jwtUtil.validateAccessToken(null, testUsername);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("空のトークンを拒否する")
        void shouldRejectEmptyToken() {
            Boolean isValid = jwtUtil.validateAccessToken("", testUsername);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("検証時のnullユーザー名を処理する")
        void shouldHandleNullUsernameInValidation() {
            Boolean isValid = jwtUtil.validateAccessToken(validToken, null);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("検証時の空ユーザー名を処理する")
        void shouldHandleEmptyUsernameInValidation() {
            String emptyUsernameToken = jwtUtil.generateAccessToken("", Role.USER);
            Boolean isValid = jwtUtil.validateAccessToken(emptyUsernameToken, "");

            // JJWTライブラリの仕様により、空文字列で生成されたトークンは
            // subjectがnullになるため、空文字列での検証は失敗する
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("トークン有効期限処理")
    class TokenExpiration {

        @Test
        @DisplayName("期限切れでないトークンを正しく検出する")
        void shouldDetectNonExpiredTokenCorrectly() {
            String token = jwtUtil.generateAccessToken(testUsername, Role.USER);
            Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);
            Date now = new Date();

            assertThat(expiration).isAfter(now);
        }

        @Test
        @DisplayName("期限切れトークンを正しく検出する")
        void shouldDetectExpiredTokenCorrectly() throws InterruptedException {
            // 非常に短い有効期限のutilを作成
            JwtUtil shortExpirationUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortExpirationUtil, "secret", testSecret);
            ReflectionTestUtils.setField(shortExpirationUtil, "accessTokenExpiration", 1L); // 1 second
            ReflectionTestUtils.setField(shortExpirationUtil, "refreshTokenExpiration", 1L);
            ReflectionTestUtils.setField(shortExpirationUtil, "slidingRefreshExpiration", false);

            String token = shortExpirationUtil.generateAccessToken(testUsername, Role.USER);

            // トークンの有効期限切れを待つ
            Thread.sleep(1500L); // 1.5秒待機

            // クレームを抽出しようとするとExpiredJwtExceptionがスローされるべき
            assertThatThrownBy(() -> jwtUtil.extractUsername(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("正しい有効期限時間設定を返す")
        void shouldReturnCorrectExpirationTimeConfiguration() {
            Long expirationTime = jwtUtil.getAccessTokenExpiration();

            assertThat(expirationTime).isEqualTo(testExpiration);
        }
    }

    @Nested
    @DisplayName("セキュリティとエッジケース")
    class SecurityAndEdgeCases {

        @Test
        @DisplayName("同じ秘密鍵で一貫した署名鍵を使用する")
        void shouldUseConsistentSigningKeyForSameSecret() {
            String token1 = jwtUtil.generateAccessToken(testUsername, Role.USER);
            String token2 = jwtUtil.generateAccessToken(testUsername, Role.USER);

            // 両方のトークンが同じutilインスタンスで検証可能であるべき
            assertThat(jwtUtil.validateAccessToken(token1, testUsername)).isTrue();
            assertThat(jwtUtil.validateAccessToken(token2, testUsername)).isTrue();
        }

        @Test
        @DisplayName("非常に長いユーザー名を処理する")
        void shouldHandleVeryLongUsername() {
            String longUsername = "a".repeat(1000);
            String token = jwtUtil.generateAccessToken(longUsername, Role.USER);

            assertThat(jwtUtil.extractUsername(token)).isEqualTo(longUsername);
            assertThat(jwtUtil.validateAccessToken(token, longUsername)).isTrue();
        }

        @Test
        @DisplayName("ユーザー名のUnicode文字を処理する")
        void shouldHandleUnicodeCharactersInUsername() {
            String unicodeUsername = "ユーザー名テスト123";
            String token = jwtUtil.generateAccessToken(unicodeUsername, Role.USER);

            assertThat(jwtUtil.extractUsername(token)).isEqualTo(unicodeUsername);
            assertThat(jwtUtil.validateAccessToken(token, unicodeUsername)).isTrue();
        }

        @Test
        @DisplayName("ユーザー名の特殊文字を処理する")
        void shouldHandleSpecialCharactersInUsername() {
            String specialUsername = "user@domain.com+test!#$%";
            String token = jwtUtil.generateAccessToken(specialUsername, Role.USER);

            assertThat(jwtUtil.extractUsername(token)).isEqualTo(specialUsername);
            assertThat(jwtUtil.validateAccessToken(token, specialUsername)).isTrue();
        }
    }
}