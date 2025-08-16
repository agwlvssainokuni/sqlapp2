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

import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.EmailTemplateService.EmailContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * メール通知サービス。
 * ユーザーの登録・承認・拒否に関するメール通知を処理します。
 * 開発環境ではMailPit、本番環境では実際のSMTPサーバーを使用します。
 */
@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    public EmailNotificationService(JavaMailSender mailSender, EmailTemplateService emailTemplateService) {
        this.mailSender = mailSender;
        this.emailTemplateService = emailTemplateService;
    }

    /**
     * ユーザー登録完了通知メールを送信します。
     * 
     * @param user 登録されたユーザー
     * @param language 送信する言語（null可、その場合はフォールバック言語を使用）
     */
    public void sendRegistrationNotification(User user, String language) {
        try {
            String lang = language != null ? language : "en";
            EmailContent content = emailTemplateService.createRegistrationNotification(user, lang);
            sendEmail(user.getEmail(), content);
            logger.info("ユーザー登録通知メールを送信しました: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("ユーザー登録通知メールの送信に失敗しました: {}", user.getEmail(), e);
        }
    }

    /**
     * ユーザー承認完了通知メールを送信します。
     * 
     * @param user 承認されたユーザー
     * @param language 送信する言語（null可、その場合はフォールバック言語を使用）
     */
    public void sendApprovalNotification(User user, String language) {
        try {
            String lang = language != null ? language : "en";
            EmailContent content = emailTemplateService.createApprovalNotification(user, lang);
            sendEmail(user.getEmail(), content);
            logger.info("ユーザー承認通知メールを送信しました: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("ユーザー承認通知メールの送信に失敗しました: {}", user.getEmail(), e);
        }
    }

    /**
     * ユーザー申請拒否通知メールを送信します。
     * 
     * @param user 拒否されたユーザー
     * @param reason 拒否理由
     * @param language 送信する言語（null可、その場合はフォールバック言語を使用）
     */
    public void sendRejectionNotification(User user, String reason, String language) {
        try {
            String lang = language != null ? language : "en";
            EmailContent content = emailTemplateService.createRejectionNotification(user, reason, lang);
            sendEmail(user.getEmail(), content);
            logger.info("ユーザー拒否通知メールを送信しました: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("ユーザー拒否通知メールの送信に失敗しました: {}", user.getEmail(), e);
        }
    }

    /**
     * 実際にメールを送信します。
     * 
     * @param to 送信先メールアドレス
     * @param content メールコンテンツ
     */
    private void sendEmail(String to, EmailContent content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(content.subject());
        message.setText(content.body());
        
        // BCCが設定されている場合は追加
        if (content.bcc() != null && !content.bcc().isEmpty()) {
            message.setBcc(content.bcc());
        }
        
        mailSender.send(message);
        logger.debug("メール送信完了: to={}, subject={}", to, content.subject());
    }
}