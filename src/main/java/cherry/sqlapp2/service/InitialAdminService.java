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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 初期管理者自動作成サービス。
 * アプリケーション起動時に管理者アカウントが存在しない場合、
 * 設定ファイルの情報を基に初期管理者を自動作成します。
 */
@Service
public class InitialAdminService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitialAdminService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.user.admin.enabled:true}")
    private boolean adminCreationEnabled;
    
    @Value("${app.user.admin.username:admin}")
    private String adminUsername;
    
    @Value("${app.user.admin.password:admin123}")
    private String adminPassword;
    
    @Value("${app.user.admin.email:admin@sqlapp2.local}")
    private String adminEmail;

    public InitialAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * アプリケーション起動時に実行される初期管理者作成処理。
     * 管理者アカウントが存在しない場合のみ作成します。
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminCreationEnabled) {
            logger.info("初期管理者自動作成機能は無効になっています");
            return;
        }

        long adminCount = userRepository.countByRole(Role.ADMIN);
        if (adminCount > 0) {
            logger.info("管理者アカウントが既に存在します ({}件)", adminCount);
            return;
        }

        // 管理者アカウントが存在しない場合、初期管理者を作成
        String hashedPassword = passwordEncoder.encode(adminPassword);
        User adminUser = new User(adminUsername, hashedPassword, adminEmail, Role.ADMIN, UserStatus.APPROVED);
        
        userRepository.save(adminUser);
        logger.info("初期管理者アカウントを作成しました: {}", adminUsername);
    }
}