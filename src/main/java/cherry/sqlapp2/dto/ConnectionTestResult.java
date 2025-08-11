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
package cherry.sqlapp2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ConnectionTestResult(
    @JsonProperty("success") boolean success,
    @JsonProperty("message") String message,
    @JsonProperty("testedAt") LocalDateTime testedAt
) {
    public ConnectionTestResult(boolean success, String message) {
        this(success, message, LocalDateTime.now());
    }

    public static ConnectionTestResult createSuccess() {
        return new ConnectionTestResult(true, "Connection test successful");
    }

    public static ConnectionTestResult createFailure(String message) {
        return new ConnectionTestResult(false, message);
    }
}