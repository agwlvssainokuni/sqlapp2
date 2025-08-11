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
import cherry.sqlapp2.entity.User;
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

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userService.findByUsername(request.getUsername()).get();

        String token = jwtUtil.generateToken(user.getUsername());
        Long expiresIn = jwtUtil.getExpirationTime();

        LoginResult loginResult = new LoginResult(token, expiresIn, new LoginUser(user));
        return ApiResponse.success(loginResult);
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
