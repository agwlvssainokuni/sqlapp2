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
import cherry.sqlapp2.service.RefreshTokenService;
import cherry.sqlapp2.service.UserService;
import cherry.sqlapp2.util.JwtUtil;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

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
        return ApiResponse.success(loginResult);
    }

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

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        // Revoke the refresh token
        boolean revoked = refreshTokenService.revokeToken(request.refreshToken());

        if (!revoked) {
            return ApiResponse.error(List.of("Invalid refresh token"));
        }

        return ApiResponse.success(null);
    }

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
