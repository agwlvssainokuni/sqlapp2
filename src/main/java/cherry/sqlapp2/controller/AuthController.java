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

package cherry.sqlapp2.controller;

import cherry.sqlapp2.dto.*;
import cherry.sqlapp2.entity.RefreshToken;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.MetricsService;
import cherry.sqlapp2.service.RefreshTokenService;
import cherry.sqlapp2.service.UserService;
import cherry.sqlapp2.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * ユーザ認証・認可の管理を行うコントローラクラス。
 * ログイン、ログアウト、トークンリフレッシュ、ユーザ登録などの
 * 認証に関連する操作を提供します。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and authorization operations")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final MetricsService metricsService;

    @Autowired
    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService,
            MetricsService metricsService
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.metricsService = metricsService;
    }

    /**
     * ユーザのログイン認証を行います。
     * ユーザ名とパスワードを検証し、成功時にはアクセストークンとリフレッシュトークンを生成して返します。
     * 
     * @param request ログイン認証情報（ユーザ名とパスワード）
     * @return ログイン結果（トークン情報とユーザ情報）を含むAPIレスポンス
     */
    @Operation(
            summary = "User login",
            description = "Authenticate user and return access token",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login credentials",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "Login Example",
                                    value = "{\"username\": \"admin\", \"password\": \"password123\"}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userService.findByUsername(request.getUsername()).get();

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        Long accessExpiresIn = jwtUtil.getAccessTokenExpiration();
        Long refreshExpiresIn = jwtUtil.getRefreshTokenExpiration();

        LoginResult loginResult = new LoginResult(
                accessToken,
                refreshToken.getToken(),
                accessExpiresIn,
                refreshExpiresIn,
                new LoginUser(user)
        );

        // Record successful login metric
        metricsService.recordUserLogin(user.getUsername());

        return ApiResponse.success(loginResult);
    }

    /**
     * リフレッシュトークンを使用してアクセストークンを更新します。
     * 期限切れ前のアクセストークンを新しいトークンに置き換えます。
     * 
     * @param request リフレッシュトークンを含むリクエスト
     * @return 新しいアクセストークンとリフレッシュトークンを含むAPIレスポンス
     */
    @Operation(
            summary = "Refresh access token",
            description = "Generate a new access token using a refresh token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token"
            )
    })
    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResult> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenService.useRefreshToken(request.refreshToken());

        if (refreshTokenOpt.isEmpty()) {
            return ApiResponse.error(List.of("Invalid or expired refresh token"));
        }

        RefreshToken refreshToken = refreshTokenOpt.get();
        User user = refreshToken.getUser();

        // Check if user can refresh token (for account deactivation control)
        if (!refreshTokenService.canRefreshToken(user)) {
            return ApiResponse.error(List.of("Token refresh not allowed for this user"));
        }

        // Validate the refresh token with JWT
        if (!jwtUtil.validateRefreshToken(request.refreshToken(), user.getUsername())) {
            return ApiResponse.error(List.of("Invalid refresh token"));
        }

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername());

        // Generate new refresh token if not using sliding expiration
        String newRefreshToken = request.refreshToken();
        Long refreshExpiresIn = jwtUtil.getRefreshTokenExpiration();

        if (!jwtUtil.isSlidingRefreshExpiration()) {
            // Create a new refresh token and revoke the old one
            RefreshToken newRefreshTokenEntity = refreshTokenService.createRefreshToken(user);
            refreshTokenService.revokeToken(refreshToken);
            newRefreshToken = newRefreshTokenEntity.getToken();
        }

        RefreshTokenResult result = new RefreshTokenResult(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getAccessTokenExpiration(),
                refreshExpiresIn
        );

        return ApiResponse.success(result);
    }

    /**
     * ユーザのログアウト処理を行います。
     * 指定されたリフレッシュトークンを無効化してセッションを終了します。
     * 
     * @param request 無効化するリフレッシュトークンを含むリクエスト
     * @return ログアウト完了を示すAPIレスポンス
     */
    @Operation(
            summary = "User logout",
            description = "Logout user and invalidate refresh token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logout successful"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid refresh token"
            )
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        // Revoke the refresh token
        boolean revoked = refreshTokenService.revokeToken(request.refreshToken());

        if (!revoked) {
            return ApiResponse.error(List.of("Invalid refresh token"));
        }

        return ApiResponse.success(null);
    }

    /**
     * 全デバイスからのログアウト処理を行います。
     * 認証されたユーザのすべてのリフレッシュトークンを無効化します。
     * 
     * @param authentication 認証情報
     * @return 全ログアウト完了を示すAPIレスポンス
     */
    @Operation(
            summary = "Logout from all devices",
            description = "Logout user from all devices by invalidating all refresh tokens"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(Authentication authentication) {
        User user = Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .flatMap(userService::findByUsername)
                .get();

        refreshTokenService.revokeAllUserTokens(user);
        return ApiResponse.success(null);
    }

    /**
     * 新規ユーザの登録を行います。
     * ユーザ名の重複チェック、パスワードのハッシュ化を行い、新しいユーザアカウントを作成します。
     * 
     * @param request ユーザ登録情報（ユーザ名、パスワード、メールアドレス）
     * @return 登録されたユーザ情報を含むAPIレスポンス
     */
    @Operation(
            summary = "User registration",
            description = "Register a new user account",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User registration data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserRegistrationRequest.class),
                            examples = @ExampleObject(
                                    name = "Registration Example",
                                    value = "{\"username\": \"newuser\", \"password\": \"password123\", \"email\": \"user@example.com\"}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data or username already exists",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginUser>> register(@Valid @RequestBody UserRegistrationRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
        );
        LoginUser loginUser = new LoginUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(loginUser)
        );
    }

    /**
     * 現在認証されているユーザの情報を取得します。
     * JWTトークンから認証情報を抽出し、ユーザのプロフィール情報を返します。
     * 
     * @return 認証されたユーザの情報を含むAPIレスポンス
     */
    @Operation(
            summary = "Get current user information",
            description = "Retrieve authenticated user's profile information"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User information retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ApiResponse<LoginUser> getCurrentUser() {
        User user = Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .flatMap(userService::findByUsername)
                .get();
        LoginUser loginUser = new LoginUser(user);
        return ApiResponse.success(loginUser);
    }

}
