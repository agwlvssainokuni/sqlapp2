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
import cherry.sqlapp2.entity.EmailTemplate;
import cherry.sqlapp2.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * メールテンプレート管理機能を提供するコントローラークラス。
 * 管理者専用の機能でメールテンプレートの作成、更新、削除、取得を行います。
 */
@RestController
@RequestMapping("/api/admin/email-templates")
@Tag(name = "Email Template Management", description = "管理者向けメールテンプレート管理API")
@PreAuthorize("hasRole('ADMIN')")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @Autowired
    public EmailTemplateController(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    /**
     * 全メールテンプレートを取得します。
     *
     * @param authentication 認証情報
     * @return 全メールテンプレートのリスト
     */
    @GetMapping
    @Operation(summary = "全メールテンプレート取得", description = "全てのメールテンプレートを取得します")
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getAllTemplates(
            @Parameter(hidden = true) Authentication authentication) {
        List<EmailTemplate> templates = emailTemplateService.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * 指定されたテンプレートキーのメールテンプレートを取得します。
     *
     * @param templateKey テンプレートキー
     * @param authentication 認証情報
     * @return 指定されたテンプレートキーのメールテンプレートリスト
     */
    @GetMapping("/{templateKey}")
    @Operation(summary = "テンプレートキー別メールテンプレート取得", 
               description = "指定されたテンプレートキーの全言語メールテンプレートを取得します")
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getTemplatesByKey(
            @PathVariable String templateKey,
            @Parameter(hidden = true) Authentication authentication) {
        List<EmailTemplate> templates = emailTemplateService.getTemplatesByKey(templateKey);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * 新しいメールテンプレートを作成します。
     *
     * @param request テンプレート作成リクエスト
     * @param authentication 認証情報
     * @return 作成されたメールテンプレート
     */
    @PostMapping
    @Operation(summary = "メールテンプレート作成", description = "新しいメールテンプレートを作成します")
    public ResponseEntity<ApiResponse<EmailTemplate>> createTemplate(
            @Valid @RequestBody EmailTemplateRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            EmailTemplate template = emailTemplateService.createTemplate(
                request.templateKey, 
                request.language, 
                request.subject, 
                request.body, 
                request.bcc
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(template));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(List.of(e.getMessage())));
        }
    }

    /**
     * メールテンプレートを更新します。
     *
     * @param id テンプレートID
     * @param request テンプレート更新リクエスト
     * @param authentication 認証情報
     * @return 更新されたメールテンプレート
     */
    @PutMapping("/{id}")
    @Operation(summary = "メールテンプレート更新", description = "既存のメールテンプレートを更新します")
    public ResponseEntity<ApiResponse<EmailTemplate>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody EmailTemplateRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            EmailTemplate template = emailTemplateService.updateTemplate(
                id, 
                request.subject, 
                request.body, 
                request.bcc
            );
            return ResponseEntity.ok(ApiResponse.success(template));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(List.of(e.getMessage())));
        }
    }

    /**
     * メールテンプレートを削除します。
     *
     * @param id テンプレートID
     * @param authentication 認証情報
     * @return 削除結果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "メールテンプレート削除", description = "指定されたメールテンプレートを削除します")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            emailTemplateService.deleteTemplate(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(List.of(e.getMessage())));
        }
    }

    /**
     * メールテンプレート作成・更新用のリクエストクラス。
     */
    public static class EmailTemplateRequest {
        @NotBlank(message = "Template key is required")
        @Size(max = 100, message = "Template key must not exceed 100 characters")
        public String templateKey;

        @NotBlank(message = "Language is required")
        @Size(max = 10, message = "Language must not exceed 10 characters")
        public String language;

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must not exceed 200 characters")
        public String subject;

        @NotBlank(message = "Body is required")
        public String body;

        @Size(max = 200, message = "BCC must not exceed 200 characters")
        public String bcc;
    }
}