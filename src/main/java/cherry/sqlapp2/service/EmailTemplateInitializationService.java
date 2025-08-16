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
import cherry.sqlapp2.repository.EmailTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * メールテンプレートの初期データ登録サービス。
 * アプリケーション起動時に必要なメールテンプレートがなければ自動作成します。
 */
@Service
public class EmailTemplateInitializationService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateInitializationService.class);

    private final EmailTemplateRepository emailTemplateRepository;
    private final boolean autoCreateTemplates;
    private final String defaultBcc;

    public EmailTemplateInitializationService(
            EmailTemplateRepository emailTemplateRepository,
            @Value("${app.email.auto-create-templates:true}") boolean autoCreateTemplates,
            @Value("${app.email.default-bcc:admin@sqlapp2.local}") String defaultBcc
    ) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.autoCreateTemplates = autoCreateTemplates;
        this.defaultBcc = defaultBcc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoCreateTemplates) {
            logger.info("Email template auto-creation is disabled");
            return;
        }

        logger.info("Initializing email templates...");
        
        createTemplateIfNotExists("user-registration", "en", 
            "SqlApp2 - User Registration Complete",
            "Hello {{username}},\n\n" +
            "Your account has been registered successfully on {{registeredAt}}.\n" +
            "Please wait for administrator approval before you can log in.\n\n" +
            "Thank you for using SqlApp2.",
            defaultBcc);

        createTemplateIfNotExists("user-registration", "ja", 
            "SqlApp2 - ユーザー登録完了",
            "{{username}}様\n\n" +
            "{{registeredAt}}にアカウント登録が完了しました。\n" +
            "管理者の承認をお待ちください。承認後にログインが可能になります。\n\n" +
            "SqlApp2をご利用いただきありがとうございます。",
            defaultBcc);

        createTemplateIfNotExists("user-approved", "en", 
            "SqlApp2 - Account Approved",
            "Hello {{username}},\n\n" +
            "Your account has been approved on {{approvedAt}}.\n" +
            "You can now log in to SqlApp2 and start using the application.\n\n" +
            "Thank you for using SqlApp2.",
            defaultBcc);

        createTemplateIfNotExists("user-approved", "ja", 
            "SqlApp2 - アカウント承認完了",
            "{{username}}様\n\n" +
            "{{approvedAt}}にアカウントが承認されました。\n" +
            "SqlApp2にログインしてアプリケーションをご利用いただけます。\n\n" +
            "SqlApp2をご利用いただきありがとうございます。",
            defaultBcc);

        createTemplateIfNotExists("user-rejected", "en", 
            "SqlApp2 - Account Application Rejected",
            "Hello {{username}},\n\n" +
            "Unfortunately, your account application has been rejected on {{rejectedAt}}.\n" +
            "Reason: {{reason}}\n\n" +
            "If you have any questions, please contact the administrator.\n\n" +
            "Thank you for your interest in SqlApp2.",
            defaultBcc);

        createTemplateIfNotExists("user-rejected", "ja", 
            "SqlApp2 - アカウント申請拒否",
            "{{username}}様\n\n" +
            "申し訳ございませんが、{{rejectedAt}}にアカウント申請が拒否されました。\n" +
            "理由: {{reason}}\n\n" +
            "ご質問がございましたら管理者にお問い合わせください。\n\n" +
            "SqlApp2にご関心をお寄せいただきありがとうございました。",
            defaultBcc);

        logger.info("Email template initialization completed");
    }

    /**
     * テンプレートが存在しない場合に作成します。
     */
    private void createTemplateIfNotExists(String templateKey, String language, String subject, String body, String bcc) {
        if (!emailTemplateRepository.existsByTemplateKeyAndLanguage(templateKey, language)) {
            EmailTemplate template = new EmailTemplate(templateKey, language, subject, body, bcc);
            emailTemplateRepository.save(template);
            logger.info("Created email template: {} ({})", templateKey, language);
        } else {
            logger.debug("Email template already exists: {} ({})", templateKey, language);
        }
    }
}