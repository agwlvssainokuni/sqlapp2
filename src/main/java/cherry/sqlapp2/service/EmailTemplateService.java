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

import cherry.sqlapp2.entity.EmailTemplate;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * メールテンプレート処理サービス。
 * データベースから国際化対応のメールテンプレートを取得し、
 * 動的な値を置換してメールコンテンツを生成します。
 */
@Service
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    
    @Value("${app.email.fallback-language:en}")
    private String fallbackLanguage;
    
    @Value("${app.email.default-bcc:admin@sqlapp2.local}")
    private String defaultBcc;
    
    @Autowired
    public EmailTemplateService(EmailTemplateRepository emailTemplateRepository) {
        this.emailTemplateRepository = emailTemplateRepository;
    }

    /**
     * ユーザー登録通知メールのコンテンツを生成します。
     */
    public EmailContent createRegistrationNotification(User user, String language) {
        EmailTemplate template = getTemplate("user-registration", language);
        Map<String, String> variables = Map.of(
            "username", user.getUsername(),
            "email", user.getEmail(),
            "registeredAt", formatDateTime(user.getCreatedAt())
        );
        
        return new EmailContent(
            replaceVariables(template.getSubject(), variables),
            replaceVariables(template.getBody(), variables),
            template.getBcc() != null ? template.getBcc() : defaultBcc
        );
    }

    /**
     * ユーザー承認通知メールのコンテンツを生成します。
     */
    public EmailContent createApprovalNotification(User user, String language) {
        EmailTemplate template = getTemplate("user-approved", language);
        Map<String, String> variables = Map.of(
            "username", user.getUsername(),
            "approvedAt", formatDateTime(LocalDateTime.now())
        );
        
        return new EmailContent(
            replaceVariables(template.getSubject(), variables),
            replaceVariables(template.getBody(), variables),
            template.getBcc() != null ? template.getBcc() : defaultBcc
        );
    }

    /**
     * ユーザー拒否通知メールのコンテンツを生成します。
     */
    public EmailContent createRejectionNotification(User user, String reason, String language) {
        EmailTemplate template = getTemplate("user-rejected", language);
        Map<String, String> variables = Map.of(
            "username", user.getUsername(),
            "rejectedAt", formatDateTime(LocalDateTime.now()),
            "reason", reason != null ? reason : "Application did not meet approval criteria"
        );
        
        return new EmailContent(
            replaceVariables(template.getSubject(), variables),
            replaceVariables(template.getBody(), variables),
            template.getBcc() != null ? template.getBcc() : defaultBcc
        );
    }

    /**
     * テンプレートを取得します。指定された言語がない場合はフォールバック言語を使用します。
     */
    private EmailTemplate getTemplate(String templateKey, String language) {
        Optional<EmailTemplate> template = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, language);
        if (template.isPresent()) {
            return template.get();
        }
        
        // フォールバック言語で再試行
        Optional<EmailTemplate> fallbackTemplate = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, fallbackLanguage);
        if (fallbackTemplate.isPresent()) {
            return fallbackTemplate.get();
        }
        
        throw new RuntimeException("Email template not found: " + templateKey + " for language: " + language + " or fallback: " + fallbackLanguage);
    }
    
    /**
     * テンプレート内の変数を置換します。
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }
    
    /**
     * 日時を読みやすい形式でフォーマットします。
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * メールコンテンツを表すレコード。
     */
    public record EmailContent(String subject, String body, String bcc) {}
}