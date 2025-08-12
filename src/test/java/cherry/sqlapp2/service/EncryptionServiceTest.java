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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EncryptionService - AES-GCM暗号化")
class EncryptionServiceTest {

    private final String validBase64Key = EncryptionService.generateBase64Key();
    private final String testPlaintext = "sensitive password data";

    @Nested
    @DisplayName("暗号化処理")
    class Encryption {

        @Test
        @DisplayName("平文データを正常に暗号化する")
        void shouldEncryptPlaintextSuccessfully() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);

            // When
            String encrypted = encryptionService.encrypt(testPlaintext);

            // Then
            assertThat(encrypted).isNotNull()
                    .isNotBlank()
                    .isNotEqualTo(testPlaintext);
            
            // Base64形式であることを確認
            assertThatCode(() -> Base64.getDecoder().decode(encrypted))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("同じ平文でも異なる暗号化結果を生成する（IV使用確認）")
        void shouldGenerateDifferentEncryptedResultsForSamePlaintext() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);

            // When
            String encrypted1 = encryptionService.encrypt(testPlaintext);
            String encrypted2 = encryptionService.encrypt(testPlaintext);

            // Then
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("空文字列を処理する")
        void shouldHandleEmptyString() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);

            // When
            String encrypted = encryptionService.encrypt("");

            // Then
            assertThat(encrypted).isEmpty();
        }

        @Test
        @DisplayName("null値を処理する")
        void shouldHandleNullValue() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);

            // When
            String encrypted = encryptionService.encrypt(null);

            // Then
            assertThat(encrypted).isNull();
        }

        @Test
        @DisplayName("非常に長い文字列を暗号化する")
        void shouldEncryptVeryLongString() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String longText = "a".repeat(10000);

            // When
            String encrypted = encryptionService.encrypt(longText);

            // Then
            assertThat(encrypted).isNotNull()
                    .isNotBlank()
                    .isNotEqualTo(longText);
        }

        @Test
        @DisplayName("Unicode文字を含む文字列を暗号化する")
        void shouldEncryptUnicodeCharacters() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String unicodeText = "パスワード123🔒🔑";

            // When
            String encrypted = encryptionService.encrypt(unicodeText);

            // Then
            assertThat(encrypted).isNotNull()
                    .isNotBlank()
                    .isNotEqualTo(unicodeText);
        }
    }

    @Nested
    @DisplayName("復号化処理")
    class Decryption {

        @Test
        @DisplayName("暗号化されたデータを正常に復号化する")
        void shouldDecryptEncryptedDataSuccessfully() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String encrypted = encryptionService.encrypt(testPlaintext);

            // When
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertThat(decrypted).isEqualTo(testPlaintext);
        }

        @Test
        @DisplayName("空文字列を処理する")
        void shouldHandleEmptyString() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);

            // When
            String decrypted = encryptionService.decrypt("");

            // Then
            assertThat(decrypted).isEmpty();
        }

        @Test
        @DisplayName("null値を処理する")
        void shouldHandleNullValue() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);

            // When
            String decrypted = encryptionService.decrypt(null);

            // Then
            assertThat(decrypted).isNull();
        }

        @Test
        @DisplayName("不正なBase64文字列で例外をスローする")
        void shouldThrowExceptionForInvalidBase64() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String invalidBase64 = "invalid base64 string!@#$";

            // When & Then
            assertThatThrownBy(() -> encryptionService.decrypt(invalidBase64))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to decrypt data");
        }

        @Test
        @DisplayName("改ざんされたデータで例外をスローする")
        void shouldThrowExceptionForTamperedData() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String encrypted = encryptionService.encrypt(testPlaintext);
            
            // 暗号化データを改ざん
            byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
            encryptedBytes[0] = (byte) (encryptedBytes[0] ^ 0xFF); // 最初のバイトを反転
            String tamperedEncrypted = Base64.getEncoder().encodeToString(encryptedBytes);

            // When & Then
            assertThatThrownBy(() -> encryptionService.decrypt(tamperedEncrypted))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to decrypt data");
        }

        @Test
        @DisplayName("短すぎるデータで例外をスローする")
        void shouldThrowExceptionForTooShortData() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String tooShortData = Base64.getEncoder().encodeToString(new byte[5]); // IVよりも短い

            // When & Then
            assertThatThrownBy(() -> encryptionService.decrypt(tooShortData))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to decrypt data");
        }
    }

    @Nested
    @DisplayName("暗号化・復号化ラウンドトリップ")
    class RoundTrip {

        @Test
        @DisplayName("暗号化後復号化で元のデータを復元する")
        void shouldRestoreOriginalDataAfterEncryptionDecryption() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);

            // When
            String encrypted = encryptionService.encrypt(testPlaintext);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertThat(decrypted).isEqualTo(testPlaintext);
        }

        @Test
        @DisplayName("複数回のラウンドトリップでデータの整合性を保つ")
        void shouldMaintainDataIntegrityThroughMultipleRoundTrips() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String originalData = testPlaintext;

            // When & Then
            for (int i = 0; i < 10; i++) {
                String encrypted = encryptionService.encrypt(originalData);
                String decrypted = encryptionService.decrypt(encrypted);
                assertThat(decrypted).isEqualTo(originalData);
            }
        }

        @Test
        @DisplayName("特殊文字を含むデータのラウンドトリップ")
        void shouldHandleSpecialCharactersInRoundTrip() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String specialCharsData = "!@#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`";

            // When
            String encrypted = encryptionService.encrypt(specialCharsData);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertThat(decrypted).isEqualTo(specialCharsData);
        }

        @Test
        @DisplayName("改行文字を含むデータのラウンドトリップ")
        void shouldHandleNewlineCharactersInRoundTrip() {
            // Given
            EncryptionService encryptionService = new EncryptionService(validBase64Key);
            String multilineData = "Line 1\nLine 2\r\nLine 3\rLine 4";

            // When
            String encrypted = encryptionService.encrypt(multilineData);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertThat(decrypted).isEqualTo(multilineData);
        }
    }

    @Nested
    @DisplayName("キー管理とエラーハンドリング")
    class KeyManagementAndErrorHandling {

        @Test
        @DisplayName("無効なキーで例外をスローする")
        void shouldThrowExceptionForInvalidKey() {
            // Given
            String invalidKey = "invalid-key";

            // When & Then
            assertThatThrownBy(() -> new EncryptionService(invalidKey).encrypt(testPlaintext))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Failed to encrypt data");
        }

        @Test
        @DisplayName("空のキーで例外をスローする")
        void shouldThrowExceptionForEmptyKey() {
            // Given
            String emptyKey = "";

            // When & Then
            assertThatThrownBy(() -> new EncryptionService(emptyKey).encrypt(testPlaintext))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("Encryption key is not configured. Please set app.encryption.key property.");
        }

        @Test
        @DisplayName("nullキーで例外をスローする")
        void shouldThrowExceptionForNullKey() {
            // Given
            String nullKey = null;

            // When & Then
            assertThatThrownBy(() -> new EncryptionService(nullKey).encrypt(testPlaintext))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("Encryption key is not configured. Please set app.encryption.key property.");
        }

        @Test
        @DisplayName("異なるキーで暗号化されたデータは復号化できない")
        void shouldNotDecryptDataEncryptedWithDifferentKey() {
            // Given
            String key1 = EncryptionService.generateBase64Key();
            String key2 = EncryptionService.generateBase64Key();
            EncryptionService service1 = new EncryptionService(key1);
            EncryptionService service2 = new EncryptionService(key2);

            String encrypted = service1.encrypt(testPlaintext);

            // When & Then
            assertThatThrownBy(() -> service2.decrypt(encrypted))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to decrypt data");
        }
    }

    @Nested
    @DisplayName("キー生成")
    class KeyGeneration {

        @Test
        @DisplayName("有効なBase64キーを生成する")
        void shouldGenerateValidBase64Key() {
            // When
            String generatedKey = EncryptionService.generateBase64Key();

            // Then
            assertThat(generatedKey).isNotNull().isNotBlank();
            
            // Base64デコードできることを確認
            assertThatCode(() -> Base64.getDecoder().decode(generatedKey))
                    .doesNotThrowAnyException();
            
            // キーの長さが適切であることを確認（AES-256 = 32バイト = 44文字のBase64）
            byte[] keyBytes = Base64.getDecoder().decode(generatedKey);
            assertThat(keyBytes).hasSize(32); // 256 bits / 8 = 32 bytes
        }

        @Test
        @DisplayName("生成されたキーが毎回異なる")
        void shouldGenerateDifferentKeysEachTime() {
            // When
            String key1 = EncryptionService.generateBase64Key();
            String key2 = EncryptionService.generateBase64Key();

            // Then
            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("生成されたキーで暗号化・復号化が動作する")
        void shouldWorkWithGeneratedKey() {
            // Given
            String generatedKey = EncryptionService.generateBase64Key();
            EncryptionService encryptionService = new EncryptionService(generatedKey);

            // When
            String encrypted = encryptionService.encrypt(testPlaintext);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertThat(decrypted).isEqualTo(testPlaintext);
        }
    }
}