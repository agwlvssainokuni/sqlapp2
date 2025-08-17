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

import cherry.sqlapp2.entity.Role;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.entity.UserStatus;
import cherry.sqlapp2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ユーザ管理機能を提供するサービスクラス。
 * ユーザの作成、認証、検索、承認管理などの操作を担当します。
 * パスワードのハッシュ化、メトリクス記録、メール通知も行います。
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MetricsService metricsService;
    private final EmailNotificationService emailNotificationService;
    
    @Value("${app.user.auto-approve-in-tests:false}")
    private boolean autoApproveInTests;

    @Autowired
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MetricsService metricsService,
            EmailNotificationService emailNotificationService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.metricsService = metricsService;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * 新しいユーザを作成します。
     * ユーザ名とメールアドレスの重複チェックを行い、パスワードをハッシュ化して保存します。
     * 作成されたユーザーは承認待ち状態となり、登録通知メールが送信されます。
     *
     * @param username ユーザ名
     * @param password パスワード（平文）
     * @param email    メールアドレス
     * @return 作成されたユーザ
     */
    public User createUser(String username, String password, String email) {
        return createUser(username, password, email, "en");
    }

    /**
     * 新しいユーザを作成します（言語設定付き）。
     * ユーザ名とメールアドレスの重複チェックを行い、パスワードをハッシュ化して保存します。
     * 作成されたユーザーは承認待ち状態となり、登録通知メールが送信されます。
     * 言語設定は承認・拒絶メール送信時の言語選択に使用されます。
     *
     * @param username ユーザ名
     * @param password パスワード（平文）
     * @param email    メールアドレス
     * @param language 言語設定（en/ja）
     * @return 作成されたユーザ
     */
    public User createUser(String username, String password, String email, String language) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, encodedPassword, email, language); // デフォルトでPENDING状態
        
        // 統合テスト環境では自動承認
        if (autoApproveInTests) {
            user.setStatus(UserStatus.APPROVED);
        }
        
        User savedUser = userRepository.save(user);

        // Record user registration metric
        metricsService.recordUserRegistration();

        // Send registration notification email with user's language
        emailNotificationService.sendRegistrationNotification(savedUser, savedUser.getLanguage());

        return savedUser;
    }

    /**
     * ユーザ名でユーザを検索します。
     *
     * @param username 検索するユーザ名
     * @return ユーザ（存在する場合）
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * ユーザのパスワードを検証します。
     *
     * @param user        ユーザ
     * @param rawPassword 検証する平文パスワード
     * @return パスワードが正しい場合true
     */
    @Transactional(readOnly = true)
    public boolean validatePassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 承認待ちユーザーの一覧を取得します。
     *
     * @param pageable ページング情報
     * @return 承認待ちユーザーのページ
     */
    @Transactional(readOnly = true)
    public Page<User> getPendingUsers(Pageable pageable) {
        return userRepository.findByStatus(UserStatus.PENDING, pageable);
    }

    /**
     * ユーザーを承認します。
     *
     * @param userId 承認するユーザーID
     * @return 承認されたユーザー
     */
    public User approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("User is not in pending status");
        }

        user.setStatus(UserStatus.APPROVED);
        User savedUser = userRepository.save(user);

        // Send approval notification email with user's language
        emailNotificationService.sendApprovalNotification(savedUser, savedUser.getLanguage());

        return savedUser;
    }

    /**
     * ユーザー申請を拒否します。
     *
     * @param userId 拒否するユーザーID
     * @param reason 拒否理由
     * @return 拒否されたユーザー
     */
    public User rejectUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("User is not in pending status");
        }

        user.setStatus(UserStatus.REJECTED);
        User savedUser = userRepository.save(user);

        // Send rejection notification email with user's language
        emailNotificationService.sendRejectionNotification(savedUser, reason, savedUser.getLanguage());

        return savedUser;
    }

    /**
     * 指定されたIDのユーザーを取得します。
     *
     * @param userId ユーザーID
     * @return ユーザー
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
}