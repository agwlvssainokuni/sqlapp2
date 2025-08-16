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
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.RefreshTokenRepository;
import cherry.sqlapp2.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * リフレッシュトークン管理機能を提供するサービスクラス。
 * JWTリフレッシュトークンの作成、利用、無効化、期限管理を行います。
 * セキュアな認証フローとトークンライフサイクル管理を担当します。
 */
@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 指定されたユーザに対して新しいリフレッシュトークンを作成します。
     *
     * @param user トークンを作成するユーザ
     * @return 作成されたリフレッシュトークン
     */
    public RefreshToken createRefreshToken(User user) {
        // Revoke existing active tokens for security (optional, but recommended for single session)
        revokeAllUserTokens(user);

        String tokenValue = jwtUtil.generateRefreshToken(user.getUsername());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration());

        RefreshToken refreshToken = new RefreshToken(tokenValue, user, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * トークン値でリフレッシュトークンを検索します。
     *
     * @param token トークン値
     * @return リフレッシュトークン（存在する場合）
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * リフレッシュトークンを検証し、有効な場合に返します。
     *
     * @param token 検証するトークン値
     * @return 有効なリフレッシュトークン（存在し有効な場合）
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        return findByToken(token)
                .filter(RefreshToken::isActive);
    }

    /**
     * リフレッシュトークンを使用して新しいアクセストークンを生成します。
     * 最終使用日時を更新し、必要に応じて有効期限を延長します。
     *
     * @param token 使用するリフレッシュトークン値
     * @return 更新されたリフレッシュトークン（有効な場合）
     */
    public Optional<RefreshToken> useRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = validateRefreshToken(token);

        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            refreshToken.updateLastUsed();

            // If sliding expiration is enabled, extend the refresh token expiration
            if (jwtUtil.isSlidingRefreshExpiration()) {
                LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration());
                refreshToken.setExpiresAt(newExpiresAt);
            }

            return Optional.of(refreshTokenRepository.save(refreshToken));
        }

        return Optional.empty();
    }

    /**
     * Revoke a specific refresh token
     */
    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Revoke a specific refresh token by token value
     */
    public boolean revokeToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = findByToken(token);
        if (refreshTokenOpt.isPresent()) {
            revokeToken(refreshTokenOpt.get());
            return true;
        }
        return false;
    }

    /**
     * Revoke all refresh tokens for a user
     */
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllTokensByUser(user);
    }

    /**
     * Clean up expired tokens (should be called periodically)
     */
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    /**
     * Clean up revoked tokens (should be called periodically)
     */
    public void cleanupRevokedTokens() {
        refreshTokenRepository.deleteRevokedTokens();
    }

    /**
     * Validate that a refresh token belongs to the specified user
     */
    public boolean isTokenOwner(RefreshToken refreshToken, User user) {
        return refreshToken.getUser().getId().equals(user.getId());
    }

    /**
     * Check if a user can refresh their token (used for account deactivation control)
     * This method can be extended to implement business logic for token refresh control
     */
    public boolean canRefreshToken(User user) {
        // For now, all users can refresh tokens
        // This can be extended to check user status, account locks, etc.
        return true;
    }
}