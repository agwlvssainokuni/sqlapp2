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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * メトリクス収集サービス
 * SQL実行性能、ユーザー活動、エラー率などのカスタムメトリクスを管理
 */
@Service
public class MetricsService {

    private final Counter sqlExecutionCounter;
    private final Counter sqlExecutionErrorCounter;
    private final Timer sqlExecutionTimer;
    private final Counter userLoginCounter;
    private final Counter userRegistrationCounter;
    private final Counter databaseConnectionCounter;
    private final Counter databaseConnectionErrorCounter;
    private final Counter queryManagementCounter;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalResultRows = new AtomicLong(0);
    private final AtomicInteger currentActiveUsers = new AtomicInteger(0);

    public MetricsService(
            MeterRegistry meterRegistry
    ) {

        // SQL実行関連メトリクス
        this.sqlExecutionCounter = Counter.builder("sql_executions_total")
                .description("Total number of SQL executions")
                .register(meterRegistry);

        this.sqlExecutionErrorCounter = Counter.builder("sql_execution_errors_total")
                .description("Total number of SQL execution errors")
                .register(meterRegistry);

        this.sqlExecutionTimer = Timer.builder("sql_execution_duration")
                .description("SQL execution duration")
                .register(meterRegistry);

        // ユーザー関連メトリクス
        this.userLoginCounter = Counter.builder("user_logins_total")
                .description("Total number of user logins")
                .register(meterRegistry);

        this.userRegistrationCounter = Counter.builder("user_registrations_total")
                .description("Total number of user registrations")
                .register(meterRegistry);

        // データベース接続関連メトリクス
        this.databaseConnectionCounter = Counter.builder("database_connections_total")
                .description("Total number of database connection attempts")
                .register(meterRegistry);

        this.databaseConnectionErrorCounter = Counter.builder("database_connection_errors_total")
                .description("Total number of database connection errors")
                .register(meterRegistry);

        // クエリ管理関連メトリクス
        this.queryManagementCounter = Counter.builder("query_management_operations_total")
                .description("Total number of query management operations")
                .register(meterRegistry);

        // ゲージメトリクス（現在の状態を示す）
        Gauge.builder("database_active_connections", activeConnections, AtomicInteger::doubleValue)
                .description("Number of active database connections")
                .register(meterRegistry);

        Gauge.builder("sql_execution_total_rows", totalResultRows, AtomicLong::doubleValue)
                .description("Total number of rows returned by SQL executions")
                .register(meterRegistry);

        Gauge.builder("users_active_current", currentActiveUsers, AtomicInteger::doubleValue)
                .description("Number of currently active users")
                .register(meterRegistry);
    }

    /**
     * SQL実行メトリクスを記録
     */
    public void recordSqlExecution(long durationMs, int resultRows, boolean isError) {
        sqlExecutionCounter.increment();
        sqlExecutionTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        totalResultRows.addAndGet(resultRows);

        if (isError) {
            sqlExecutionErrorCounter.increment();
        }
    }

    /**
     * SQL実行タイマーを開始
     */
    public Timer.Sample startSqlExecutionTimer() {
        return Timer.start();
    }

    /**
     * SQL実行タイマーを停止してメトリクスに記録
     */
    public void recordSqlExecutionComplete(Timer.Sample sample, int resultRows, boolean isError) {
        sample.stop(sqlExecutionTimer);
        sqlExecutionCounter.increment();
        totalResultRows.addAndGet(resultRows);

        if (isError) {
            sqlExecutionErrorCounter.increment();
        }
    }

    /**
     * ユーザーログインを記録
     */
    public void recordUserLogin(String username) {
        userLoginCounter.increment();
        currentActiveUsers.incrementAndGet();
    }

    /**
     * ユーザー登録を記録
     */
    public void recordUserRegistration() {
        userRegistrationCounter.increment();
    }

    /**
     * ユーザーログアウトを記録
     */
    public void recordUserLogout() {
        currentActiveUsers.updateAndGet(current -> Math.max(0, current - 1));
    }

    /**
     * データベース接続試行を記録
     */
    public void recordDatabaseConnectionAttempt(String dbType, boolean isError) {
        databaseConnectionCounter.increment();

        if (isError) {
            databaseConnectionErrorCounter.increment();
        } else {
            activeConnections.incrementAndGet();
        }
    }

    /**
     * データベース接続終了を記録
     */
    public void recordDatabaseConnectionClosed() {
        activeConnections.updateAndGet(current -> Math.max(0, current - 1));
    }

    /**
     * クエリ管理操作を記録
     */
    public void recordQueryManagementOperation(String operationType) {
        queryManagementCounter.increment();
    }

    /**
     * アクティブな接続数を取得
     */
    public int getActiveConnections() {
        return activeConnections.get();
    }

    /**
     * 現在のアクティブユーザー数を取得
     */
    public int getCurrentActiveUsers() {
        return currentActiveUsers.get();
    }

    /**
     * 総結果行数を取得
     */
    public long getTotalResultRows() {
        return totalResultRows.get();
    }
}