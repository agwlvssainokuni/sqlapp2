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
import cherry.sqlapp2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * ユーザ管理機能を提供するサービスクラス。
 * ユーザの作成、認証、検索などの操作を担当します。
 * パスワードのハッシュ化やメトリクス記録も行います。
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MetricsService metricsService;

    @Autowired
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MetricsService metricsService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.metricsService = metricsService;
    }

    /**
     * 新しいユーザを作成します。
     * ユーザ名とメールアドレスの重複チェックを行い、パスワードをハッシュ化して保存します。
     *
     * @param username ユーザ名
     * @param password パスワード（平文）
     * @param email    メールアドレス
     * @return 作成されたユーザ
     */
    public User createUser(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, encodedPassword, email);
        User savedUser = userRepository.save(user);

        // Record user registration metric
        metricsService.recordUserRegistration();

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
}