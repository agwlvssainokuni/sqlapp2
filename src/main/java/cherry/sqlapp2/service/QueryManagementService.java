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

import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.QueryHistory;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.exception.ResourceNotFoundException;
import cherry.sqlapp2.exception.ValidationException;
import cherry.sqlapp2.repository.QueryHistoryRepository;
import cherry.sqlapp2.repository.SavedQueryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * クエリ管理機能を提供するサービスクラス。
 * 保存クエリの作成・更新・削除、クエリ実行履歴の管理、
 * 統計情報の取得などのクエリライフサイクル全体を管理します。
 * パラメータ定義の処理とJSONシリアライゼーションも担当します。
 */
@Service
@Transactional
public class QueryManagementService {

    private final SavedQueryRepository savedQueryRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final MetricsService metricsService;
    private final int defaultPeriodDays;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public QueryManagementService(
            SavedQueryRepository savedQueryRepository,
            QueryHistoryRepository queryHistoryRepository,
            MetricsService metricsService,
            @Value("${app.query-history.default-period-days:30}") int defaultPeriodDays
    ) {
        this.savedQueryRepository = savedQueryRepository;
        this.queryHistoryRepository = queryHistoryRepository;
        this.metricsService = metricsService;
        this.defaultPeriodDays = defaultPeriodDays;
    }

    // ==================== Saved Queries Management ====================

    /**
     * 新しいクエリを保存します。
     *
     * @param name                 クエリ名
     * @param sqlContent           SQL内容
     * @param description          説明
     * @param parameterDefinitions パラメータ定義
     * @param sharingScope         共有範囲
     * @param user                 ユーザ
     * @param defaultConnection    デフォルト接続
     * @return 保存されたクエリ
     */
    public SavedQuery saveQuery(String name, String sqlContent, String description,
                                Map<String, String> parameterDefinitions,
                                SavedQuery.SharingScope sharingScope,
                                User user, DatabaseConnection defaultConnection) {

        if (savedQueryRepository.existsByUserAndName(user, name)) {
            throw new IllegalArgumentException("Query name already exists for this user: " + name);
        }

        SavedQuery savedQuery = new SavedQuery();
        savedQuery.setName(name);
        savedQuery.setSqlContent(sqlContent);
        savedQuery.setDescription(description);
        savedQuery.setSharingScope(sharingScope != null ? sharingScope : SavedQuery.SharingScope.PRIVATE);
        savedQuery.setUser(user);
        savedQuery.setDefaultConnection(defaultConnection);

        if (parameterDefinitions != null && !parameterDefinitions.isEmpty()) {
            try {
                savedQuery.setParameterDefinitions(objectMapper.writeValueAsString(parameterDefinitions));
            } catch (JsonProcessingException e) {
                throw new ValidationException("Failed to serialize parameter definitions", e);
            }
        }

        SavedQuery result = savedQueryRepository.save(savedQuery);
        
        // Record query management operation
        metricsService.recordQueryManagementOperation("SAVE");
        
        return result;
    }

    /**
     * 保存されたクエリを更新します。
     *
     * @param queryId              クエリID
     * @param name                 クエリ名
     * @param sqlContent           SQL内容
     * @param description          説明
     * @param parameterDefinitions パラメータ定義
     * @param sharingScope         共有範囲
     * @param defaultConnection    デフォルト接続
     * @param user                 ユーザ
     * @return 更新されたクエリ
     */
    public SavedQuery updateSavedQuery(Long queryId, String name, String sqlContent,
                                       String description, Map<String, String> parameterDefinitions,
                                       SavedQuery.SharingScope sharingScope,
                                       DatabaseConnection defaultConnection, User user) {

        SavedQuery savedQuery = savedQueryRepository.findById(queryId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved query not found: " + queryId));

        if (!savedQuery.getUser().getId().equals(user.getId())) {
            throw new ValidationException("Access denied: You can only update your own queries");
        }

        if (!savedQuery.getName().equals(name) && savedQueryRepository.existsByUserAndName(user, name)) {
            throw new IllegalArgumentException("Query name already exists for this user: " + name);
        }

        savedQuery.setName(name);
        savedQuery.setSqlContent(sqlContent);
        savedQuery.setDescription(description);
        savedQuery.setSharingScope(sharingScope);
        savedQuery.setDefaultConnection(defaultConnection);

        if (parameterDefinitions != null && !parameterDefinitions.isEmpty()) {
            try {
                savedQuery.setParameterDefinitions(objectMapper.writeValueAsString(parameterDefinitions));
            } catch (JsonProcessingException e) {
                throw new ValidationException("Failed to serialize parameter definitions", e);
            }
        } else {
            savedQuery.setParameterDefinitions(null);
        }

        SavedQuery result = savedQueryRepository.save(savedQuery);
        
        // Record query management operation
        metricsService.recordQueryManagementOperation("UPDATE");
        
        return result;
    }

    @Transactional(readOnly = true)
    public List<SavedQuery> getUserQueries(User user) {
        return savedQueryRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<SavedQuery> getPublicQueries() {
        return savedQueryRepository.findPublicQueries();
    }

    @Transactional(readOnly = true)
    public Optional<SavedQuery> getAccessibleQuery(Long queryId, User user) {
        return savedQueryRepository.findAccessibleQuery(queryId, user);
    }

    public void deleteSavedQuery(Long queryId, User user) {
        SavedQuery savedQuery = savedQueryRepository.findById(queryId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved query not found: " + queryId));

        if (!savedQuery.getUser().getId().equals(user.getId())) {
            throw new ValidationException("Access denied: You can only delete your own queries");
        }

        savedQueryRepository.delete(savedQuery);
        
        // Record query management operation
        metricsService.recordQueryManagementOperation("DELETE");
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Map<String, String> getParameterDefinitions(Long queryId, User user) {
        SavedQuery savedQuery = savedQueryRepository.findAccessibleQuery(queryId, user)
                .orElseThrow(() -> new RuntimeException("Saved query not found or not accessible: " + queryId));

        if (savedQuery.getParameterDefinitions() == null || savedQuery.getParameterDefinitions().trim().isEmpty()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(savedQuery.getParameterDefinitions(), Map.class);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to deserialize parameter definitions", e);
        }
    }

    // ==================== Query History Management ====================

    public QueryHistory recordExecution(String sqlContent, Map<String, Object> parameterValues,
                                        long executionTimeMs, Integer resultCount, boolean isSuccessful,
                                        String errorMessage, User user, DatabaseConnection connection,
                                        SavedQuery savedQuery) {

        QueryHistory history = new QueryHistory();
        history.setSqlContent(sqlContent);
        history.setExecutionTimeMs(executionTimeMs);
        history.setResultCount(resultCount);
        history.setIsSuccessful(isSuccessful);
        history.setErrorMessage(errorMessage);
        history.setUser(user);
        history.setConnection(connection);
        history.setSavedQuery(savedQuery);

        if (parameterValues != null && !parameterValues.isEmpty()) {
            try {
                history.setParameterValues(objectMapper.writeValueAsString(parameterValues));
            } catch (JsonProcessingException e) {
                throw new ValidationException("Failed to serialize parameter values", e);
            }
        }

        // Update saved query execution statistics if this execution is for a saved query
        if (savedQuery != null && isSuccessful) {
            savedQuery.incrementExecutionCount();
            savedQueryRepository.save(savedQuery);
        }

        return queryHistoryRepository.save(history);
    }

    /**
     * ユーザのクエリ履歴を取得します（デフォルト期間）。
     * 設定で指定された期間内の履歴を返します。
     */
    @Transactional(readOnly = true)
    public Page<QueryHistory> getUserQueryHistory(User user, Pageable pageable) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(defaultPeriodDays);
        return queryHistoryRepository.findByUserAndExecutedAtAfter(user, fromDate, pageable);
    }

    /**
     * ユーザのクエリ履歴を指定された期間で取得します。
     * FROM日付は必須、TO日付は任意です。
     */
    @Transactional(readOnly = true)
    public Page<QueryHistory> getUserQueryHistoryWithDateRange(User user, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        if (toDate != null) {
            return queryHistoryRepository.findByUserAndExecutedAtBetween(user, fromDate, toDate, pageable);
        } else {
            return queryHistoryRepository.findByUserAndExecutedAtAfter(user, fromDate, pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<QueryHistory> getQueryHistoryAfter(User user, LocalDateTime fromDate, Pageable pageable) {
        return queryHistoryRepository.findByUserAndExecutedAtAfter(user, fromDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<QueryHistory> searchQueryHistory(User user, String searchTerm, Pageable pageable) {
        return queryHistoryRepository.searchByUserAndSqlContent(user, searchTerm, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<QueryHistory> getQueryHistoryById(Long historyId, User user) {
        return queryHistoryRepository.findByIdAndUser(historyId, user);
    }

    @Transactional(readOnly = true)
    public Page<QueryHistory> getSuccessfulQueries(User user, Pageable pageable) {
        return queryHistoryRepository.findByUserAndIsSuccessful(user, true, pageable);
    }

    @Transactional(readOnly = true)
    public Page<QueryHistory> getFailedQueries(User user, Pageable pageable) {
        return queryHistoryRepository.findByUserAndIsSuccessful(user, false, pageable);
    }

    // ==================== Statistics ====================

    @Transactional(readOnly = true)
    public long getUserQueryCount(User user) {
        return savedQueryRepository.countByUser(user);
    }

    @Transactional(readOnly = true)
    public long getPublicQueryCount() {
        return savedQueryRepository.countPublicQueries();
    }

    @Transactional(readOnly = true)
    public long getUserExecutionCount(User user) {
        return queryHistoryRepository.countByUser(user);
    }

    @Transactional(readOnly = true)
    public Double getUserAverageExecutionTime(User user) {
        return queryHistoryRepository.findAverageExecutionTimeByUser(user);
    }

    @Transactional(readOnly = true)
    public long getUserFailedQueryCount(User user) {
        return queryHistoryRepository.countFailedQueriesByUser(user);
    }

    // ==================== Cleanup ====================

    @Transactional
    public void cleanupOldHistory(User user, int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        queryHistoryRepository.deleteByUserAndExecutedAtBefore(user, cutoffDate);
    }
}
