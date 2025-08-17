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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT（JSON Web Token）の生成・検証・解析機能を提供するユーティリティクラス。
 * アクセストークンとリフレッシュトークンの作成、検証、クレーム抽出などの
 * JWT関連の処理を一元管理します。HMAC-SHA256署名を使用してトークンの整合性を保証します。
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${app.jwt.refresh-token-sliding-expiration}")
    private Boolean slidingRefreshExpiration;

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String ROLE_CLAIM = "role";

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWTトークンからユーザ名を抽出します。
     * 
     * @param token JWTトークン
     * @return ユーザ名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    /**
     * アクセストークンを生成します。
     * 
     * @param username ユーザ名
     * @param role ユーザーロール
     * @return 生成されたアクセストークン
     */
    public String generateAccessToken(String username, Role role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        claims.put(ROLE_CLAIM, role.name());
        // Add a unique identifier to ensure uniqueness even for simultaneous token creation
        claims.put("jti", UUID.randomUUID().toString());
        return createToken(claims, username, accessTokenExpiration);
    }


    /**
     * リフレッシュトークンを生成します。
     * 
     * @param username ユーザ名
     * @param role ユーザーロール
     * @return 生成されたリフレッシュトークン
     */
    public String generateRefreshToken(String username, Role role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        claims.put(ROLE_CLAIM, role.name());
        // Add a unique identifier to ensure uniqueness even for simultaneous token creation
        claims.put("jti", UUID.randomUUID().toString());
        return createToken(claims, username, refreshTokenExpiration);
    }



    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateAccessToken(String token, String username) {
        try {
            final String tokenUsername = extractUsername(token);
            final String tokenType = extractTokenType(token);
            return (tokenUsername.equals(username) && 
                    ACCESS_TOKEN_TYPE.equals(tokenType) && 
                    !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateRefreshToken(String token, String username) {
        try {
            final String tokenUsername = extractUsername(token);
            final String tokenType = extractTokenType(token);
            return (tokenUsername.equals(username) && 
                    REFRESH_TOKEN_TYPE.equals(tokenType) && 
                    !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }


    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }


    public Boolean isSlidingRefreshExpiration() {
        return slidingRefreshExpiration;
    }


}
