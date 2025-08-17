/*
 * Copyright 2025 SqlApp2
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

import cherry.sqlapp2.enums.Role;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.enums.UserStatus;
import cherry.sqlapp2.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - ユーザー管理")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MetricsService metricsService;

    @Mock
    private EmailNotificationService emailNotificationService;

    private UserService userService;

    private final String testUsername = "testUser";
    private final String testPassword = "testPassword123";
    private final String testEmail = "test@example.com";
    private final String encodedPassword = "$2a$10$encoded.password.hash";

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder,
                metricsService,
                emailNotificationService
        );
    }

    @Nested
    @DisplayName("ユーザー作成")
    class UserCreation {

        @Test
        @DisplayName("新しいユーザーを正常に作成する")
        void shouldCreateNewUserSuccessfully() {
            // Given
            when(userRepository.existsByUsername(testUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);

            User expectedUser = new User(testUsername, encodedPassword, testEmail);
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);

            // When
            User result = userService.createUser(testUsername, testPassword, testEmail);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(testUsername);
            assertThat(result.getPassword()).isEqualTo(encodedPassword);
            assertThat(result.getEmail()).isEqualTo(testEmail);

            verify(userRepository).existsByUsername(testUsername);
            verify(userRepository).existsByEmail(testEmail);
            verify(passwordEncoder).encode(testPassword);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("重複するユーザー名で例外をスローする")
        void shouldThrowExceptionForDuplicateUsername() {
            // Given
            when(userRepository.existsByUsername(testUsername)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.createUser(testUsername, testPassword, testEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Username already exists");

            verify(userRepository).existsByUsername(testUsername);
            verify(userRepository, never()).existsByEmail(anyString());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("重複するメールアドレスで例外をスローする")
        void shouldThrowExceptionForDuplicateEmail() {
            // Given
            when(userRepository.existsByUsername(testUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.createUser(testUsername, testPassword, testEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists");

            verify(userRepository).existsByUsername(testUsername);
            verify(userRepository).existsByEmail(testEmail);
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("パスワードエンコーダーが正しく呼び出される")
        void shouldCallPasswordEncoderCorrectly() {
            // Given
            when(userRepository.existsByUsername(testUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(new User(testUsername, encodedPassword, testEmail));

            // When
            userService.createUser(testUsername, testPassword, testEmail);

            // Then
            verify(passwordEncoder).encode(testPassword);
        }

        @Test
        @DisplayName("言語設定付きユーザーを正常に作成する")
        void shouldCreateUserWithLanguageSuccessfully() {
            // Given
            String language = "ja";
            when(userRepository.existsByUsername(testUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);

            User expectedUser = new User(testUsername, encodedPassword, testEmail, language);
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);

            // When
            User result = userService.createUser(testUsername, testPassword, testEmail, language);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(testUsername);
            assertThat(result.getPassword()).isEqualTo(encodedPassword);
            assertThat(result.getEmail()).isEqualTo(testEmail);
            assertThat(result.getLanguage()).isEqualTo(language);

            verify(userRepository).existsByUsername(testUsername);
            verify(userRepository).existsByEmail(testEmail);
            verify(passwordEncoder).encode(testPassword);
            verify(userRepository).save(any(User.class));
            verify(emailNotificationService).sendRegistrationNotification(any(User.class), eq(language));
        }

        @Test
        @DisplayName("null言語設定でデフォルト言語を使用する")
        void shouldUseDefaultLanguageForNullLanguage() {
            // Given
            String language = null;
            when(userRepository.existsByUsername(testUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);

            User expectedUser = new User(testUsername, encodedPassword, testEmail, "en");
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);

            // When
            User result = userService.createUser(testUsername, testPassword, testEmail, language);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLanguage()).isEqualTo("en");

            verify(emailNotificationService).sendRegistrationNotification(any(User.class), eq("en"));
        }

        @Test
        @DisplayName("空のユーザー名を処理する")
        void shouldHandleEmptyUsername() {
            // Given
            String emptyUsername = "";
            when(userRepository.existsByUsername(emptyUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(new User(emptyUsername, encodedPassword, testEmail));

            // When
            User result = userService.createUser(emptyUsername, testPassword, testEmail);

            // Then
            assertThat(result.getUsername()).isEqualTo(emptyUsername);
        }

        @Test
        @DisplayName("空のパスワードを処理する")
        void shouldHandleEmptyPassword() {
            // Given
            String emptyPassword = "";
            String emptyEncodedPassword = "$2a$10$empty.encoded";
            when(userRepository.existsByUsername(testUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(emptyPassword)).thenReturn(emptyEncodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(new User(testUsername, emptyEncodedPassword, testEmail));

            // When
            User result = userService.createUser(testUsername, emptyPassword, testEmail);

            // Then
            assertThat(result.getPassword()).isEqualTo(emptyEncodedPassword);
            verify(passwordEncoder).encode(emptyPassword);
        }
    }

    @Nested
    @DisplayName("ユーザー検索")
    class UserLookup {

        @Test
        @DisplayName("存在するユーザー名でユーザーを検索する")
        void shouldFindUserByExistingUsername() {
            // Given
            User existingUser = new User(testUsername, encodedPassword, testEmail);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(existingUser));

            // When
            Optional<User> result = userService.findByUsername(testUsername);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo(testUsername);
            verify(userRepository).findByUsername(testUsername);
        }

        @Test
        @DisplayName("存在しないユーザー名で空のOptionalを返す")
        void shouldReturnEmptyOptionalForNonExistentUsername() {
            // Given
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findByUsername(testUsername);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findByUsername(testUsername);
        }

        @Test
        @DisplayName("nullユーザー名を処理する")
        void shouldHandleNullUsername() {
            // Given
            when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findByUsername(null);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findByUsername(null);
        }

        @Test
        @DisplayName("空のユーザー名を処理する")
        void shouldHandleEmptyUsername() {
            // Given
            String emptyUsername = "";
            when(userRepository.findByUsername(emptyUsername)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findByUsername(emptyUsername);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findByUsername(emptyUsername);
        }
    }

    @Nested
    @DisplayName("パスワード検証")
    class PasswordValidation {

        private User testUser;

        @BeforeEach
        void setUpUser() {
            testUser = new User(testUsername, encodedPassword, testEmail);
        }





    }

    @Nested
    @DisplayName("重複チェック")
    class DuplicationCheck {

    }

    @Nested
    @DisplayName("セキュリティとエッジケース")
    class SecurityAndEdgeCases {

        @Test
        @DisplayName("非常に長いユーザー名を処理する")
        void shouldHandleVeryLongUsername() {
            // Given
            String longUsername = "a".repeat(1000);
            when(userRepository.existsByUsername(longUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(new User(longUsername, encodedPassword, testEmail));

            // When
            User result = userService.createUser(longUsername, testPassword, testEmail);

            // Then
            assertThat(result.getUsername()).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("特殊文字を含むユーザー名を処理する")
        void shouldHandleSpecialCharactersInUsername() {
            // Given
            String specialUsername = "user@domain.com+test!#$%";
            when(userRepository.existsByUsername(specialUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(new User(specialUsername, encodedPassword, testEmail));

            // When
            User result = userService.createUser(specialUsername, testPassword, testEmail);

            // Then
            assertThat(result.getUsername()).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("Unicode文字を含むユーザー名を処理する")
        void shouldHandleUnicodeCharactersInUsername() {
            // Given
            String unicodeUsername = "ユーザー名テスト123";
            when(userRepository.existsByUsername(unicodeUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(new User(unicodeUsername, encodedPassword, testEmail));

            // When
            User result = userService.createUser(unicodeUsername, testPassword, testEmail);

            // Then
            assertThat(result.getUsername()).isEqualTo(unicodeUsername);
        }

        @Test
        @DisplayName("非常に長いパスワードを処理する")
        void shouldHandleVeryLongPassword() {
            // Given
            String longPassword = "a".repeat(1000);
            String longEncodedPassword = "$2a$10$long.encoded.password";
            when(userRepository.existsByUsername(testUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(longPassword)).thenReturn(longEncodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(new User(testUsername, longEncodedPassword, testEmail));

            // When
            User result = userService.createUser(testUsername, longPassword, testEmail);

            // Then
            assertThat(result.getPassword()).isEqualTo(longEncodedPassword);
            verify(passwordEncoder).encode(longPassword);
        }

        @Test
        @DisplayName("リポジトリ例外を適切に伝播する")
        void shouldPropagateRepositoryExceptions() {
            // Given
            when(userRepository.existsByUsername(testUsername)).thenReturn(false);
            when(userRepository.existsByEmail(testEmail)).thenReturn(false);
            when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> userService.createUser(testUsername, testPassword, testEmail))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("ユーザー承認・拒絶機能と言語連動")
    class UserApprovalAndLanguage {

        @Test
        @DisplayName("ユーザー承認時にユーザーの言語でメール送信する")
        void shouldSendApprovalEmailInUserLanguage() {
            // Given
            String userLanguage = "ja";
            User pendingUser = new User(1L, testUsername, encodedPassword, testEmail, Role.USER, UserStatus.PENDING, userLanguage);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser));
            when(userRepository.save(any(User.class))).thenReturn(pendingUser);

            // When
            User result = userService.approveUser(1L);

            // Then
            assertThat(result).isNotNull();
            verify(emailNotificationService).sendApprovalNotification(any(User.class), eq(userLanguage));
        }

        @Test
        @DisplayName("ユーザー拒絶時にユーザーの言語でメール送信する")
        void shouldSendRejectionEmailInUserLanguage() {
            // Given
            String userLanguage = "en";
            String rejectionReason = "Documentation incomplete";
            User pendingUser = new User(1L, testUsername, encodedPassword, testEmail, Role.USER, UserStatus.PENDING, userLanguage);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser));
            when(userRepository.save(any(User.class))).thenReturn(pendingUser);

            // When
            User result = userService.rejectUser(1L, rejectionReason);

            // Then
            assertThat(result).isNotNull();
            verify(emailNotificationService).sendRejectionNotification(any(User.class), eq(rejectionReason), eq(userLanguage));
        }

        @Test
        @DisplayName("日本語ユーザーの承認時に日本語メール送信する")
        void shouldSendJapaneseApprovalEmailForJapaneseUser() {
            // Given
            String userLanguage = "ja";
            User japaneseUser = new User(2L, "日本ユーザー", encodedPassword, "japanese@example.com", Role.USER, UserStatus.PENDING, userLanguage);
            
            when(userRepository.findById(2L)).thenReturn(Optional.of(japaneseUser));
            when(userRepository.save(any(User.class))).thenReturn(japaneseUser);

            // When
            User result = userService.approveUser(2L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLanguage()).isEqualTo("ja");
            verify(emailNotificationService).sendApprovalNotification(any(User.class), eq("ja"));
        }

        @Test
        @DisplayName("英語ユーザーの拒絶時に英語メール送信する")
        void shouldSendEnglishRejectionEmailForEnglishUser() {
            // Given
            String userLanguage = "en";
            String rejectionReason = "Invalid email domain";
            User englishUser = new User(3L, "englishUser", encodedPassword, "english@example.com", Role.USER, UserStatus.PENDING, userLanguage);
            
            when(userRepository.findById(3L)).thenReturn(Optional.of(englishUser));
            when(userRepository.save(any(User.class))).thenReturn(englishUser);

            // When
            User result = userService.rejectUser(3L, rejectionReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLanguage()).isEqualTo("en");
            verify(emailNotificationService).sendRejectionNotification(any(User.class), eq(rejectionReason), eq("en"));
        }
    }
}