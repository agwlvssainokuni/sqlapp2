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

import cherry.sqlapp2.dto.ApiResponse;
import cherry.sqlapp2.dto.UserDto;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理者機能のREST APIコントローラー。
 * ユーザー承認管理などの管理者限定機能を提供します。
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "管理者機能API")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 承認待ちユーザー一覧を取得します。
     */
    @GetMapping("/users/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "承認待ちユーザー一覧取得", description = "管理者承認待ちのユーザー一覧を取得します")
    public ApiResponse<Page<UserDto>> getPendingUsers(
            @Parameter(description = "ページ番号（0から開始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "ページサイズ") @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        if (!currentUser.isAdmin()) {
            return ApiResponse.error(List.of("Admin privileges required"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<User> pendingUsers = userService.getPendingUsers(pageable);
        Page<UserDto> response = pendingUsers.map(this::createUserDto);

        return ApiResponse.success(response);
    }

    /**
     * ユーザーを承認します。
     */
    @PostMapping("/users/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ユーザー承認", description = "指定されたユーザーを承認し、利用可能にします")
    public ApiResponse<UserDto> approveUser(
            @Parameter(description = "承認するユーザーID") @PathVariable Long userId,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        if (!currentUser.isAdmin()) {
            return ApiResponse.error(List.of("Admin privileges required"));
        }

        try {
            User approvedUser = userService.approveUser(userId);
            return ApiResponse.success(createUserDto(approvedUser));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(List.of(e.getMessage()));
        }
    }

    /**
     * ユーザー申請を拒否します。
     */
    @PostMapping("/users/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ユーザー申請拒否", description = "指定されたユーザーの申請を拒否します")
    public ApiResponse<UserDto> rejectUser(
            @Parameter(description = "拒否するユーザーID") @PathVariable Long userId,
            @Parameter(description = "拒否理由") @RequestBody UserRejectionRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        if (!currentUser.isAdmin()) {
            return ApiResponse.error(List.of("Admin privileges required"));
        }

        try {
            User rejectedUser = userService.rejectUser(userId, request.reason());
            return ApiResponse.success(createUserDto(rejectedUser));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(List.of(e.getMessage()));
        }
    }

    /**
     * 認証情報から現在のユーザーを取得します。
     */
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * UserエンティティをUserDTOに変換します。
     */
    private UserDto createUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * ユーザー拒否リクエストDTO。
     */
    public record UserRejectionRequest(String reason) {}
}