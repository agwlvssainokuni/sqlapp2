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

package cherry.sqlapp2.config;

import cherry.sqlapp2.repository.DatabaseConnectionRepository;
import cherry.sqlapp2.repository.UserRepository;
import cherry.sqlapp2.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * カスタムヘルスチェックインジケーター
 * アプリケーション固有のヘルスチェック項目を提供
 */
public class CustomHealthIndicators {

    /**
     * 内部データベース接続チェック
     */
    @Component
    public static class InternalDatabaseHealthIndicator implements HealthIndicator {

        private final DataSource dataSource;
        private final UserRepository userRepository;
        private final DatabaseConnectionRepository connectionRepository;

        @Autowired
        public InternalDatabaseHealthIndicator(
                DataSource dataSource,
                UserRepository userRepository,
                DatabaseConnectionRepository connectionRepository
        ) {
            this.dataSource = dataSource;
            this.userRepository = userRepository;
            this.connectionRepository = connectionRepository;
        }

        @Override
        public Health health() {
            try (Connection connection = dataSource.getConnection()) {
                Health.Builder builder = Health.up();

                // 基本的な接続テスト
                if (!connection.isValid(5)) {
                    return Health.down()
                            .withDetail("reason", "Database connection is not valid")
                            .build();
                }

                // データベース基本情報
                builder.withDetail("database", connection.getMetaData().getDatabaseProductName())
                       .withDetail("version", connection.getMetaData().getDatabaseProductVersion())
                       .withDetail("url", connection.getMetaData().getURL());

                // リポジトリ動作確認（簡単なカウント）
                try {
                    long userCount = userRepository.count();
                    long connectionCount = connectionRepository.count();
                    
                    builder.withDetail("users_count", userCount)
                           .withDetail("connections_count", connectionCount);
                } catch (Exception ex) {
                    builder.withDetail("repository_error", ex.getMessage());
                }

                return builder.build();

            } catch (SQLException ex) {
                return Health.down()
                        .withDetail("error", ex.getMessage())
                        .withDetail("sql_state", ex.getSQLState())
                        .withDetail("error_code", ex.getErrorCode())
                        .build();
            }
        }
    }

    /**
     * アプリケーションメトリクスヘルスチェック
     */
    @Component
    public static class ApplicationMetricsHealthIndicator implements HealthIndicator {

        private final MetricsService metricsService;

        @Autowired
        public ApplicationMetricsHealthIndicator(MetricsService metricsService) {
            this.metricsService = metricsService;
        }

        @Override
        public Health health() {
            try {
                Health.Builder builder = Health.up();

                // メトリクス情報を取得
                int activeConnections = metricsService.getActiveConnections();
                int activeUsers = metricsService.getCurrentActiveUsers();
                long totalRows = metricsService.getTotalResultRows();

                builder.withDetail("active_database_connections", activeConnections)
                       .withDetail("active_users", activeUsers)
                       .withDetail("total_sql_result_rows", totalRows);

                // しきい値チェック（設定可能にすることも可能）
                if (activeConnections > 100) {
                    builder.withDetail("warning", "High number of active database connections");
                }

                return builder.build();

            } catch (Exception ex) {
                return Health.down()
                        .withDetail("error", ex.getMessage())
                        .withDetail("cause", ex.getCause() != null ? ex.getCause().toString() : "Unknown")
                        .build();
            }
        }
    }

    /**
     * システムリソースヘルスチェック
     */
    @Component
    public static class SystemResourceHealthIndicator implements HealthIndicator {

        @Override
        public Health health() {
            Runtime runtime = Runtime.getRuntime();
            
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

            Health.Builder builder = Health.up();

            builder.withDetail("max_memory_mb", maxMemory / 1024 / 1024)
                   .withDetail("total_memory_mb", totalMemory / 1024 / 1024)
                   .withDetail("free_memory_mb", freeMemory / 1024 / 1024)
                   .withDetail("used_memory_mb", usedMemory / 1024 / 1024)
                   .withDetail("memory_usage_percent", String.format("%.2f", memoryUsagePercent))
                   .withDetail("available_processors", runtime.availableProcessors());

            // メモリ使用率が90%を超える場合は警告
            if (memoryUsagePercent > 90) {
                builder.withDetail("warning", "High memory usage detected");
            }

            // メモリ使用率が95%を超える場合は危険状態
            if (memoryUsagePercent > 95) {
                return builder.down()
                        .withDetail("error", "Critical memory usage level")
                        .build();
            }

            return builder.build();
        }
    }
}