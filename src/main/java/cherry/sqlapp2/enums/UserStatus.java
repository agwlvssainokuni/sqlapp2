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

package cherry.sqlapp2.enums;

/**
 * ユーザー承認状態を表す列挙型。
 * ユーザー登録後の承認ワークフローの状態を管理します。
 */
public enum UserStatus {
    /** 承認待ち */
    PENDING,
    /** 承認済み */
    APPROVED,
    /** 拒否 */
    REJECTED
}