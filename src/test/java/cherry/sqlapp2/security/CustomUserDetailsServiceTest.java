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
package cherry.sqlapp2.security;

import cherry.sqlapp2.entity.Role;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.entity.UserStatus;
import cherry.sqlapp2.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService - ユーザー詳細サービス")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService customUserDetailsService;

    private final String testUsername = "testUser";
    private final String testPassword = "hashedPassword123";
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        customUserDetailsService = new CustomUserDetailsService();
        ReflectionTestUtils.setField(customUserDetailsService, "userRepository", userRepository);
    }

    @Nested
    @DisplayName("ユーザー詳細の読み込み")
    class UserDetailsLoading {

        @Test
        @DisplayName("存在するユーザーの詳細を正常に読み込む")
        void shouldLoadUserDetailsSuccessfully() {
            // Given
            User user = new User(testUsername, testPassword, testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(testUsername);
            assertThat(userDetails.getPassword()).isEqualTo(testPassword);
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();

            verify(userRepository).findByUsername(testUsername);
        }

        @Test
        @DisplayName("存在しないユーザーでUsernameNotFoundExceptionをスローする")
        void shouldThrowUsernameNotFoundExceptionForNonExistentUser() {
            // Given
            String nonExistentUsername = "nonExistentUser";
            when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(nonExistentUsername))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: " + nonExistentUsername);

            verify(userRepository).findByUsername(nonExistentUsername);
        }

        @Test
        @DisplayName("特殊文字を含むユーザー名を処理する")
        void shouldHandleSpecialCharactersInUsername() {
            // Given
            String specialUsername = "user@domain.com+test!#$%";
            User user = new User(specialUsername, testPassword, testEmail);
            when(userRepository.findByUsername(specialUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(specialUsername);

            // Then
            assertThat(userDetails.getUsername()).isEqualTo(specialUsername);
            verify(userRepository).findByUsername(specialUsername);
        }

        @Test
        @DisplayName("Unicode文字を含むユーザー名を処理する")
        void shouldHandleUnicodeCharactersInUsername() {
            // Given
            String unicodeUsername = "ユーザー名テスト123";
            User user = new User(unicodeUsername, testPassword, testEmail);
            when(userRepository.findByUsername(unicodeUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(unicodeUsername);

            // Then
            assertThat(userDetails.getUsername()).isEqualTo(unicodeUsername);
            verify(userRepository).findByUsername(unicodeUsername);
        }

        @Test
        @DisplayName("非常に長いユーザー名を処理する")
        void shouldHandleVeryLongUsername() {
            // Given
            String longUsername = "a".repeat(1000);
            User user = new User(longUsername, testPassword, testEmail);
            when(userRepository.findByUsername(longUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(longUsername);

            // Then
            assertThat(userDetails.getUsername()).isEqualTo(longUsername);
            verify(userRepository).findByUsername(longUsername);
        }

        @Test
        @DisplayName("空文字列のユーザー名でUsernameNotFoundExceptionをスローする")
        void shouldThrowExceptionForEmptyUsername() {
            // Given
            String emptyUsername = "";
            when(userRepository.findByUsername(emptyUsername)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(emptyUsername))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: " + emptyUsername);

            verify(userRepository).findByUsername(emptyUsername);
        }

        @Test
        @DisplayName("nullユーザー名でUsernameNotFoundExceptionをスローする")
        void shouldThrowExceptionForNullUsername() {
            // Given
            when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: null");

            verify(userRepository).findByUsername(null);
        }
    }

    @Nested
    @DisplayName("権限とロール管理")
    class AuthoritiesAndRoles {

        @Test
        @DisplayName("全てのユーザーにROLE_USER権限を付与する")
        void shouldGrantRoleUserToAllUsers() {
            // Given
            User user = new User(testUsername, testPassword, testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("複数のユーザーが同じ権限を持つ")
        void shouldGrantSameAuthoritiesToAllUsers() {
            // Given
            User user1 = new User("user1", testPassword, "user1@example.com");
            User user2 = new User("user2", testPassword, "user2@example.com");
            
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user1));
            when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));

            // When
            UserDetails userDetails1 = customUserDetailsService.loadUserByUsername("user1");
            UserDetails userDetails2 = customUserDetailsService.loadUserByUsername("user2");

            // Then
            assertThat(userDetails1.getAuthorities()).isEqualTo(userDetails2.getAuthorities());
            assertThat(userDetails1.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            assertThat(userDetails2.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("パスワードとセキュリティ")
    class PasswordAndSecurity {

        @Test
        @DisplayName("エンコードされたパスワードを正しく返す")
        void shouldReturnEncodedPassword() {
            // Given
            String encodedPassword = "$2a$10$encoded.password.hash";
            User user = new User(testUsername, encodedPassword, testEmail);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
        }

        @Test
        @DisplayName("空のパスワードを処理する")
        void shouldHandleEmptyPassword() {
            // Given
            String emptyPassword = "";
            User user = new User(testUsername, emptyPassword, testEmail);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails.getPassword()).isEqualTo(emptyPassword);
        }

        @Test
        @DisplayName("nullパスワードでIllegalArgumentExceptionをスローする")
        void shouldThrowExceptionForNullPassword() {
            // Given
            User user = new User(testUsername, null, testEmail);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(testUsername))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot pass null or empty values to constructor");

            verify(userRepository).findByUsername(testUsername);
        }

        @Test
        @DisplayName("非常に長いパスワードを処理する")
        void shouldHandleVeryLongPassword() {
            // Given
            String longPassword = "$2a$10$" + "a".repeat(1000);
            User user = new User(testUsername, longPassword, testEmail);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails.getPassword()).isEqualTo(longPassword);
        }
    }

    @Nested
    @DisplayName("アカウント状態")
    class AccountStatus {

        @Test
        @DisplayName("アカウントが有効であることを確認")
        void shouldReturnEnabledAccount() {
            // Given
            User user = new User(testUsername, testPassword, testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("アカウントが期限切れでないことを確認")
        void shouldReturnNonExpiredAccount() {
            // Given
            User user = new User(testUsername, testPassword, testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("アカウントがロックされていないことを確認")
        void shouldReturnNonLockedAccount() {
            // Given
            User user = new User(testUsername, testPassword, testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("認証情報が期限切れでないことを確認")
        void shouldReturnNonExpiredCredentials() {
            // Given
            User user = new User(testUsername, testPassword, testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("エラーハンドリングとエッジケース")
    class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("リポジトリ例外を適切に伝播する")
        void shouldPropagateRepositoryExceptions() {
            // Given
            when(userRepository.findByUsername(testUsername))
                    .thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(testUsername))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection error");

            verify(userRepository).findByUsername(testUsername);
        }

        @Test
        @DisplayName("大文字小文字を区別してユーザー名を処理する")
        void shouldBeCaseSensitiveForUsername() {
            // Given
            String upperCaseUsername = "TESTUSER";
            String lowerCaseUsername = "testuser";
            
            User user = new User(lowerCaseUsername, testPassword, testEmail);
            when(userRepository.findByUsername(upperCaseUsername)).thenReturn(Optional.empty());
            when(userRepository.findByUsername(lowerCaseUsername)).thenReturn(Optional.of(user));

            // When & Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(upperCaseUsername))
                    .isInstanceOf(UsernameNotFoundException.class);

            assertThatCode(() -> customUserDetailsService.loadUserByUsername(lowerCaseUsername))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("連続する呼び出しで一貫した結果を返す")
        void shouldReturnConsistentResultsForMultipleCalls() {
            // Given
            User user = new User(testUsername, testPassword, testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When
            UserDetails userDetails1 = customUserDetailsService.loadUserByUsername(testUsername);
            UserDetails userDetails2 = customUserDetailsService.loadUserByUsername(testUsername);

            // Then
            assertThat(userDetails1.getUsername()).isEqualTo(userDetails2.getUsername());
            assertThat(userDetails1.getPassword()).isEqualTo(userDetails2.getPassword());
            assertThat(userDetails1.getAuthorities()).isEqualTo(userDetails2.getAuthorities());
            
            verify(userRepository, times(2)).findByUsername(testUsername);
        }

        @Test
        @DisplayName("並行呼び出しを安全に処理する")
        void shouldHandleConcurrentCallsSafely() {
            // Given
            User user = new User(testUsername, testPassword, testEmail);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.APPROVED);
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            // When & Then - 複数回の呼び出しが例外をスローしないことを確認
            assertThatCode(() -> {
                for (int i = 0; i < 10; i++) {
                    customUserDetailsService.loadUserByUsername(testUsername);
                }
            }).doesNotThrowAnyException();

            verify(userRepository, times(10)).findByUsername(testUsername);
        }
    }
}