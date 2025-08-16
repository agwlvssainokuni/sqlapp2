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

import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.QueryHistory;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.QueryHistoryRepository;
import cherry.sqlapp2.repository.SavedQueryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryManagementService - クエリ管理サービス")
class QueryManagementServiceTest {

    @Mock
    private SavedQueryRepository savedQueryRepository;

    @Mock
    private QueryHistoryRepository queryHistoryRepository;

    @Mock
    private MetricsService metricsService;

    private QueryManagementService queryManagementService;

    private final String testUsername = "testUser";
    private final String testPassword = "hashedPassword123";
    private final String testEmail = "test@example.com";
    private final String testQueryName = "Test Query";
    private final String testSqlContent = "SELECT * FROM users WHERE id = :userId";
    private final String testDescription = "Test query description";

    private User testUser;
    private DatabaseConnection testConnection;

    @BeforeEach
    void setUp() {
        queryManagementService = new QueryManagementService(savedQueryRepository, queryHistoryRepository, metricsService, 30);
        
        testUser = new User(testUsername, testPassword, testEmail);
        ReflectionTestUtils.setField(testUser, "id", 1L);
        
        testConnection = new DatabaseConnection();
        ReflectionTestUtils.setField(testConnection, "id", 1L);
    }

    @Nested
    @DisplayName("保存済みクエリ管理")
    class SavedQueryManagement {

        @Test
        @DisplayName("新しいクエリを正常に保存する")
        void shouldSaveNewQuerySuccessfully() {
            // Given
            Map<String, String> parameterDefinitions = Map.of("userId", "INTEGER");
            SavedQuery expectedQuery = new SavedQuery();
            expectedQuery.setName(testQueryName);
            expectedQuery.setSqlContent(testSqlContent);
            expectedQuery.setDescription(testDescription);
            expectedQuery.setSharingScope(SavedQuery.SharingScope.PRIVATE);
            expectedQuery.setUser(testUser);
            expectedQuery.setDefaultConnection(testConnection);
            
            when(savedQueryRepository.existsByUserAndName(testUser, testQueryName)).thenReturn(false);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(expectedQuery);

            // When
            SavedQuery result = queryManagementService.saveQuery(
                testQueryName, testSqlContent, testDescription, parameterDefinitions,
                SavedQuery.SharingScope.PRIVATE, testUser, testConnection
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(testQueryName);
            assertThat(result.getSqlContent()).isEqualTo(testSqlContent);
            assertThat(result.getDescription()).isEqualTo(testDescription);
            assertThat(result.getSharingScope()).isEqualTo(SavedQuery.SharingScope.PRIVATE);
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getDefaultConnection()).isEqualTo(testConnection);

            verify(savedQueryRepository).existsByUserAndName(testUser, testQueryName);
            verify(savedQueryRepository).save(any(SavedQuery.class));
        }

        @Test
        @DisplayName("重複するクエリ名で例外をスローする")
        void shouldThrowExceptionForDuplicateQueryName() {
            // Given
            when(savedQueryRepository.existsByUserAndName(testUser, testQueryName)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> queryManagementService.saveQuery(
                testQueryName, testSqlContent, testDescription, null,
                SavedQuery.SharingScope.PRIVATE, testUser, testConnection
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Query name already exists for this user: " + testQueryName);

            verify(savedQueryRepository).existsByUserAndName(testUser, testQueryName);
            verify(savedQueryRepository, never()).save(any(SavedQuery.class));
        }

        @Test
        @DisplayName("パラメータ定義なしでクエリを保存する")
        void shouldSaveQueryWithoutParameterDefinitions() {
            // Given
            SavedQuery expectedQuery = new SavedQuery();
            when(savedQueryRepository.existsByUserAndName(testUser, testQueryName)).thenReturn(false);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(expectedQuery);

            // When
            SavedQuery result = queryManagementService.saveQuery(
                testQueryName, testSqlContent, testDescription, null,
                SavedQuery.SharingScope.PUBLIC, testUser, testConnection
            );

            // Then
            assertThat(result).isNotNull();
            verify(savedQueryRepository).save(argThat(query ->
                query.getName().equals(testQueryName) &&
                query.getSharingScope() == SavedQuery.SharingScope.PUBLIC
            ));
        }

        @Test
        @DisplayName("共有スコープがnullの場合デフォルトでPRIVATEになる")
        void shouldDefaultToPrivateScopeWhenNull() {
            // Given
            SavedQuery expectedQuery = new SavedQuery();
            when(savedQueryRepository.existsByUserAndName(testUser, testQueryName)).thenReturn(false);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(expectedQuery);

            // When
            queryManagementService.saveQuery(
                testQueryName, testSqlContent, testDescription, null,
                null, testUser, testConnection
            );

            // Then
            verify(savedQueryRepository).save(argThat(query ->
                query.getSharingScope() == SavedQuery.SharingScope.PRIVATE
            ));
        }

        @Test
        @DisplayName("保存済みクエリを正常に更新する")
        void shouldUpdateSavedQuerySuccessfully() {
            // Given
            Long queryId = 1L;
            String newName = "Updated Query";
            String newSqlContent = "SELECT * FROM products";
            String newDescription = "Updated description";
            Map<String, String> newParameterDefinitions = Map.of("productId", "BIGINT");
            
            SavedQuery existingQuery = new SavedQuery();
            existingQuery.setId(queryId);
            existingQuery.setName(testQueryName);
            existingQuery.setUser(testUser);
            
            SavedQuery updatedQuery = new SavedQuery();
            updatedQuery.setId(queryId);
            updatedQuery.setName(newName);
            updatedQuery.setSqlContent(newSqlContent);
            
            when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(existingQuery));
            when(savedQueryRepository.existsByUserAndName(testUser, newName)).thenReturn(false);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(updatedQuery);

            // When
            SavedQuery result = queryManagementService.updateSavedQuery(
                queryId, newName, newSqlContent, newDescription, newParameterDefinitions,
                SavedQuery.SharingScope.PUBLIC, testConnection, testUser
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(newName);
            assertThat(result.getSqlContent()).isEqualTo(newSqlContent);

            verify(savedQueryRepository).findById(queryId);
            verify(savedQueryRepository).save(any(SavedQuery.class));
        }

        @Test
        @DisplayName("存在しないクエリの更新で例外をスローする")
        void shouldThrowExceptionWhenUpdatingNonExistentQuery() {
            // Given
            Long queryId = 999L;
            when(savedQueryRepository.findById(queryId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> queryManagementService.updateSavedQuery(
                queryId, "New Name", "New SQL", "New Description", null,
                SavedQuery.SharingScope.PRIVATE, testConnection, testUser
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Saved query not found: " + queryId);

            verify(savedQueryRepository).findById(queryId);
            verify(savedQueryRepository, never()).save(any(SavedQuery.class));
        }

        @Test
        @DisplayName("他のユーザーのクエリ更新で例外をスローする")
        void shouldThrowExceptionWhenUpdatingOtherUsersQuery() {
            // Given
            Long queryId = 1L;
            User otherUser = new User("otherUser", "password", "other@example.com");
            ReflectionTestUtils.setField(otherUser, "id", 2L);
            
            SavedQuery existingQuery = new SavedQuery();
            existingQuery.setId(queryId);
            existingQuery.setUser(otherUser);
            
            when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(existingQuery));

            // When & Then
            assertThatThrownBy(() -> queryManagementService.updateSavedQuery(
                queryId, "New Name", "New SQL", "New Description", null,
                SavedQuery.SharingScope.PRIVATE, testConnection, testUser
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Access denied: You can only update your own queries");

            verify(savedQueryRepository).findById(queryId);
            verify(savedQueryRepository, never()).save(any(SavedQuery.class));
        }

        @Test
        @DisplayName("クエリ削除が正常に動作する")
        void shouldDeleteQuerySuccessfully() {
            // Given
            Long queryId = 1L;
            SavedQuery existingQuery = new SavedQuery();
            existingQuery.setId(queryId);
            existingQuery.setUser(testUser);
            
            when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(existingQuery));

            // When
            queryManagementService.deleteSavedQuery(queryId, testUser);

            // Then
            verify(savedQueryRepository).findById(queryId);
            verify(savedQueryRepository).delete(existingQuery);
        }

        @Test
        @DisplayName("他のユーザーのクエリ削除で例外をスローする")
        void shouldThrowExceptionWhenDeletingOtherUsersQuery() {
            // Given
            Long queryId = 1L;
            User otherUser = new User("otherUser", "password", "other@example.com");
            ReflectionTestUtils.setField(otherUser, "id", 2L);
            
            SavedQuery existingQuery = new SavedQuery();
            existingQuery.setId(queryId);
            existingQuery.setUser(otherUser);
            
            when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(existingQuery));

            // When & Then
            assertThatThrownBy(() -> queryManagementService.deleteSavedQuery(queryId, testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Access denied: You can only delete your own queries");

            verify(savedQueryRepository).findById(queryId);
            verify(savedQueryRepository, never()).delete(any(SavedQuery.class));
        }
    }

    @Nested
    @DisplayName("クエリ取得・検索")
    class QueryRetrievalAndSearch {

        @Test
        @DisplayName("ユーザーのクエリ一覧を取得する")
        void shouldGetUserQueries() {
            // Given
            List<SavedQuery> expectedQueries = List.of(new SavedQuery(), new SavedQuery());
            when(savedQueryRepository.findByUserOrderByUpdatedAtDesc(testUser)).thenReturn(expectedQueries);

            // When
            List<SavedQuery> result = queryManagementService.getUserQueries(testUser);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(expectedQueries);
            verify(savedQueryRepository).findByUserOrderByUpdatedAtDesc(testUser);
        }

        @Test
        @DisplayName("パブリッククエリ一覧を取得する")
        void shouldGetPublicQueries() {
            // Given
            List<SavedQuery> expectedQueries = List.of(new SavedQuery(), new SavedQuery());
            when(savedQueryRepository.findPublicQueries()).thenReturn(expectedQueries);

            // When
            List<SavedQuery> result = queryManagementService.getPublicQueries();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(expectedQueries);
            verify(savedQueryRepository).findPublicQueries();
        }

        @Test
        @DisplayName("アクセス可能なクエリを取得する")
        void shouldGetAccessibleQuery() {
            // Given
            Long queryId = 1L;
            SavedQuery expectedQuery = new SavedQuery();
            when(savedQueryRepository.findAccessibleQuery(queryId, testUser))
                    .thenReturn(Optional.of(expectedQuery));

            // When
            Optional<SavedQuery> result = queryManagementService.getAccessibleQuery(queryId, testUser);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedQuery);
            verify(savedQueryRepository).findAccessibleQuery(queryId, testUser);
        }

        @Test
        @DisplayName("パラメータ定義を正常に取得する")
        void shouldGetParameterDefinitions() {
            // Given
            Long queryId = 1L;
            Map<String, String> expectedParams = Map.of("userId", "INTEGER", "status", "VARCHAR");
            String paramJson = "{\"userId\":\"INTEGER\",\"status\":\"VARCHAR\"}";
            
            SavedQuery savedQuery = new SavedQuery();
            savedQuery.setParameterDefinitions(paramJson);
            
            when(savedQueryRepository.findAccessibleQuery(queryId, testUser))
                    .thenReturn(Optional.of(savedQuery));

            // When
            Map<String, String> result = queryManagementService.getParameterDefinitions(queryId, testUser);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsEntry("userId", "INTEGER");
            assertThat(result).containsEntry("status", "VARCHAR");
            verify(savedQueryRepository).findAccessibleQuery(queryId, testUser);
        }

        @Test
        @DisplayName("パラメータ定義がnullの場合空のMapを返す")
        void shouldReturnEmptyMapWhenParameterDefinitionsIsNull() {
            // Given
            Long queryId = 1L;
            SavedQuery savedQuery = new SavedQuery();
            savedQuery.setParameterDefinitions(null);
            
            when(savedQueryRepository.findAccessibleQuery(queryId, testUser))
                    .thenReturn(Optional.of(savedQuery));

            // When
            Map<String, String> result = queryManagementService.getParameterDefinitions(queryId, testUser);

            // Then
            assertThat(result).isEmpty();
            verify(savedQueryRepository).findAccessibleQuery(queryId, testUser);
        }

        @Test
        @DisplayName("アクセスできないクエリのパラメータ定義取得で例外をスローする")
        void shouldThrowExceptionWhenQueryNotAccessible() {
            // Given
            Long queryId = 999L;
            when(savedQueryRepository.findAccessibleQuery(queryId, testUser))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> queryManagementService.getParameterDefinitions(queryId, testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Saved query not found or not accessible: " + queryId);

            verify(savedQueryRepository).findAccessibleQuery(queryId, testUser);
        }
    }

    @Nested
    @DisplayName("クエリ実行履歴管理")
    class QueryHistoryManagement {

        @Test
        @DisplayName("実行履歴を正常に記録する")
        void shouldRecordExecutionHistory() {
            // Given
            String sqlContent = "SELECT * FROM users";
            Map<String, Object> parameterValues = Map.of("userId", 123);
            long executionTimeMs = 150L;
            Integer resultCount = 5;
            boolean isSuccessful = true;
            String errorMessage = null;
            SavedQuery savedQuery = new SavedQuery();
            
            QueryHistory expectedHistory = new QueryHistory();
            expectedHistory.setSqlContent(sqlContent);
            expectedHistory.setExecutionTimeMs(executionTimeMs);
            expectedHistory.setResultCount(resultCount);
            expectedHistory.setIsSuccessful(isSuccessful);
            
            when(queryHistoryRepository.save(any(QueryHistory.class))).thenReturn(expectedHistory);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(savedQuery);

            // When
            QueryHistory result = queryManagementService.recordExecution(
                sqlContent, parameterValues, executionTimeMs, resultCount, isSuccessful,
                errorMessage, testUser, testConnection, savedQuery
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSqlContent()).isEqualTo(sqlContent);
            assertThat(result.getExecutionTimeMs()).isEqualTo(executionTimeMs);
            assertThat(result.getResultCount()).isEqualTo(resultCount);
            assertThat(result.getIsSuccessful()).isEqualTo(isSuccessful);

            verify(queryHistoryRepository).save(any(QueryHistory.class));
            verify(savedQueryRepository).save(savedQuery); // 実行カウント更新のため
        }

        @Test
        @DisplayName("失敗した実行履歴を記録する")
        void shouldRecordFailedExecution() {
            // Given
            String sqlContent = "INVALID SQL";
            long executionTimeMs = 50L;
            boolean isSuccessful = false;
            String errorMessage = "Syntax error in SQL";
            
            QueryHistory expectedHistory = new QueryHistory();
            expectedHistory.setIsSuccessful(false);
            expectedHistory.setErrorMessage(errorMessage);
            
            when(queryHistoryRepository.save(any(QueryHistory.class))).thenReturn(expectedHistory);

            // When
            QueryHistory result = queryManagementService.recordExecution(
                sqlContent, null, executionTimeMs, null, isSuccessful,
                errorMessage, testUser, testConnection, null
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsSuccessful()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);

            verify(queryHistoryRepository).save(any(QueryHistory.class));
            verify(savedQueryRepository, never()).save(any(SavedQuery.class)); // 失敗時は実行カウント更新なし
        }

        @Test
        @DisplayName("ユーザーのクエリ履歴をページングで取得する（旧メソッド）")
        void shouldGetUserQueryHistoryWithPaging() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory(), new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 2);
            
            when(queryHistoryRepository.findByUserAndExecutedAtAfter(eq(testUser), any(LocalDateTime.class), eq(pageable)))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getUserQueryHistory(testUser, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(queryHistoryRepository).findByUserAndExecutedAtAfter(eq(testUser), any(LocalDateTime.class), eq(pageable));
        }

        @Test
        @DisplayName("特定日付以降のクエリ履歴を取得する")
        void shouldGetQueryHistoryAfterDate() {
            // Given
            LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndExecutedAtAfter(testUser, fromDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getQueryHistoryAfter(testUser, fromDate, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(queryHistoryRepository).findByUserAndExecutedAtAfter(testUser, fromDate, pageable);
        }

        @Test
        @DisplayName("デフォルト期間でクエリ履歴を取得する")
        void shouldGetUserQueryHistoryWithDefaultPeriod() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory(), new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 2);
            
            when(queryHistoryRepository.findByUserAndExecutedAtAfter(eq(testUser), any(LocalDateTime.class), eq(pageable)))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getUserQueryHistory(testUser, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(queryHistoryRepository).findByUserAndExecutedAtAfter(eq(testUser), any(LocalDateTime.class), eq(pageable));
        }

        @Test
        @DisplayName("日時範囲指定でクエリ履歴を取得する（FROM・TO両方指定）")
        void shouldGetUserQueryHistoryWithDateRangeBoth() {
            // Given
            LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
            LocalDateTime toDate = LocalDateTime.now().minusDays(1);
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndExecutedAtBetween(testUser, fromDate, toDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getUserQueryHistoryWithDateRange(testUser, fromDate, toDate, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(queryHistoryRepository).findByUserAndExecutedAtBetween(testUser, fromDate, toDate, pageable);
        }

        @Test
        @DisplayName("日時範囲指定でクエリ履歴を取得する（FROMのみ指定）")
        void shouldGetUserQueryHistoryWithDateRangeFromOnly() {
            // Given
            LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndExecutedAtAfter(testUser, fromDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getUserQueryHistoryWithDateRange(testUser, fromDate, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(queryHistoryRepository).findByUserAndExecutedAtAfter(testUser, fromDate, pageable);
        }

        @Test
        @DisplayName("成功したクエリ履歴を日時範囲指定で取得する（FROM・TO両方指定）")
        void shouldGetSuccessfulQueriesWithDateRangeBoth() {
            // Given
            LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
            LocalDateTime toDate = LocalDateTime.now().minusDays(1);
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndIsSuccessfulAndExecutedAtBetween(testUser, true, fromDate, toDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getSuccessfulQueriesWithDateRange(testUser, fromDate, toDate, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(queryHistoryRepository).findByUserAndIsSuccessfulAndExecutedAtBetween(testUser, true, fromDate, toDate, pageable);
        }

        @Test
        @DisplayName("成功したクエリ履歴を日時範囲指定で取得する（FROMのみ指定）")
        void shouldGetSuccessfulQueriesWithDateRangeFromOnly() {
            // Given
            LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndIsSuccessfulAndExecutedAtAfter(testUser, true, fromDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getSuccessfulQueriesWithDateRange(testUser, fromDate, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(queryHistoryRepository).findByUserAndIsSuccessfulAndExecutedAtAfter(testUser, true, fromDate, pageable);
        }

        @Test
        @DisplayName("失敗したクエリ履歴を日時範囲指定で取得する（FROM・TO両方指定）")
        void shouldGetFailedQueriesWithDateRangeBoth() {
            // Given
            LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
            LocalDateTime toDate = LocalDateTime.now().minusDays(1);
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndIsSuccessfulAndExecutedAtBetween(testUser, false, fromDate, toDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getFailedQueriesWithDateRange(testUser, fromDate, toDate, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(queryHistoryRepository).findByUserAndIsSuccessfulAndExecutedAtBetween(testUser, false, fromDate, toDate, pageable);
        }

        @Test
        @DisplayName("失敗したクエリ履歴を日時範囲指定で取得する（FROMのみ指定）")
        void shouldGetFailedQueriesWithDateRangeFromOnly() {
            // Given
            LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndIsSuccessfulAndExecutedAtAfter(testUser, false, fromDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getFailedQueriesWithDateRange(testUser, fromDate, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(queryHistoryRepository).findByUserAndIsSuccessfulAndExecutedAtAfter(testUser, false, fromDate, pageable);
        }

        @Test
        @DisplayName("クエリ履歴を検索する")
        void shouldSearchQueryHistory() {
            // Given
            String searchTerm = "SELECT";
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.searchByUserAndSqlContent(testUser, searchTerm, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.searchQueryHistory(testUser, searchTerm, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(queryHistoryRepository).searchByUserAndSqlContent(testUser, searchTerm, pageable);
        }

        @Test
        @DisplayName("成功したクエリのみを取得する")
        void shouldGetSuccessfulQueries() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndIsSuccessful(testUser, true, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getSuccessfulQueries(testUser, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(queryHistoryRepository).findByUserAndIsSuccessful(testUser, true, pageable);
        }

        @Test
        @DisplayName("失敗したクエリのみを取得する")
        void shouldGetFailedQueries() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<QueryHistory> historyList = List.of(new QueryHistory());
            Page<QueryHistory> expectedPage = new PageImpl<>(historyList, pageable, 1);
            
            when(queryHistoryRepository.findByUserAndIsSuccessful(testUser, false, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<QueryHistory> result = queryManagementService.getFailedQueries(testUser, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(queryHistoryRepository).findByUserAndIsSuccessful(testUser, false, pageable);
        }

        @Test
        @DisplayName("IDでクエリ履歴を取得する")
        void shouldGetQueryHistoryById() {
            // Given
            Long historyId = 1L;
            QueryHistory expectedHistory = new QueryHistory();
            when(queryHistoryRepository.findByIdAndUser(historyId, testUser))
                    .thenReturn(Optional.of(expectedHistory));

            // When
            Optional<QueryHistory> result = queryManagementService.getQueryHistoryById(historyId, testUser);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedHistory);
            verify(queryHistoryRepository).findByIdAndUser(historyId, testUser);
        }
    }

    @Nested
    @DisplayName("統計情報")
    class Statistics {

        @Test
        @DisplayName("ユーザーのクエリ数を取得する")
        void shouldGetUserQueryCount() {
            // Given
            long expectedCount = 10L;
            when(savedQueryRepository.countByUser(testUser)).thenReturn(expectedCount);

            // When
            long result = queryManagementService.getUserQueryCount(testUser);

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(savedQueryRepository).countByUser(testUser);
        }

        @Test
        @DisplayName("パブリッククエリ数を取得する")
        void shouldGetPublicQueryCount() {
            // Given
            long expectedCount = 25L;
            when(savedQueryRepository.countPublicQueries()).thenReturn(expectedCount);

            // When
            long result = queryManagementService.getPublicQueryCount();

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(savedQueryRepository).countPublicQueries();
        }

        @Test
        @DisplayName("ユーザーの実行回数を取得する")
        void shouldGetUserExecutionCount() {
            // Given
            long expectedCount = 100L;
            when(queryHistoryRepository.countByUser(testUser)).thenReturn(expectedCount);

            // When
            long result = queryManagementService.getUserExecutionCount(testUser);

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(queryHistoryRepository).countByUser(testUser);
        }

        @Test
        @DisplayName("ユーザーの平均実行時間を取得する")
        void shouldGetUserAverageExecutionTime() {
            // Given
            Double expectedAverage = 250.5;
            when(queryHistoryRepository.findAverageExecutionTimeByUser(testUser)).thenReturn(expectedAverage);

            // When
            Double result = queryManagementService.getUserAverageExecutionTime(testUser);

            // Then
            assertThat(result).isEqualTo(expectedAverage);
            verify(queryHistoryRepository).findAverageExecutionTimeByUser(testUser);
        }

        @Test
        @DisplayName("ユーザーの失敗クエリ数を取得する")
        void shouldGetUserFailedQueryCount() {
            // Given
            long expectedCount = 5L;
            when(queryHistoryRepository.countFailedQueriesByUser(testUser)).thenReturn(expectedCount);

            // When
            long result = queryManagementService.getUserFailedQueryCount(testUser);

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(queryHistoryRepository).countFailedQueriesByUser(testUser);
        }
    }

    @Nested
    @DisplayName("データクリーンアップ")
    class DataCleanup {

        @Test
        @DisplayName("古い履歴を正常にクリーンアップする")
        void shouldCleanupOldHistorySuccessfully() {
            // Given
            int daysToKeep = 30;
            LocalDateTime expectedCutoffDate = LocalDateTime.now().minusDays(daysToKeep);

            // When
            queryManagementService.cleanupOldHistory(testUser, daysToKeep);

            // Then
            verify(queryHistoryRepository).deleteByUserAndExecutedAtBefore(eq(testUser), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("異なる保持期間でクリーンアップする")
        void shouldCleanupWithDifferentRetentionPeriods() {
            // Given
            int shortRetention = 7;
            int longRetention = 90;

            // When
            queryManagementService.cleanupOldHistory(testUser, shortRetention);
            queryManagementService.cleanupOldHistory(testUser, longRetention);

            // Then
            verify(queryHistoryRepository, times(2)).deleteByUserAndExecutedAtBefore(eq(testUser), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("エラーハンドリングとエッジケース")
    class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("JSONシリアライゼーション例外を適切に処理する")
        void shouldHandleJsonSerializationException() {
            // Given - ObjectMapperをモックして例外をスローさせる
            QueryManagementService serviceWithMockMapper = new QueryManagementService(savedQueryRepository, queryHistoryRepository, metricsService, 30);
            ObjectMapper mockMapper = mock(ObjectMapper.class);
            ReflectionTestUtils.setField(serviceWithMockMapper, "objectMapper", mockMapper);

            Map<String, String> parameterDefinitions = Map.of("key", "value");
            
            when(savedQueryRepository.existsByUserAndName(testUser, testQueryName)).thenReturn(false);
            try {
                when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});
            } catch (JsonProcessingException e) {
                // This won't happen in test setup, but needed for compilation
            }

            // When & Then
            assertThatThrownBy(() -> serviceWithMockMapper.saveQuery(
                testQueryName, testSqlContent, testDescription, parameterDefinitions,
                SavedQuery.SharingScope.PRIVATE, testUser, testConnection
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to serialize parameter definitions");
        }

        @Test
        @DisplayName("空のパラメータマップを処理する")
        void shouldHandleEmptyParameterMap() {
            // Given
            Map<String, String> emptyParameters = Map.of();
            SavedQuery expectedQuery = new SavedQuery();
            
            when(savedQueryRepository.existsByUserAndName(testUser, testQueryName)).thenReturn(false);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(expectedQuery);

            // When
            SavedQuery result = queryManagementService.saveQuery(
                testQueryName, testSqlContent, testDescription, emptyParameters,
                SavedQuery.SharingScope.PRIVATE, testUser, testConnection
            );

            // Then
            assertThat(result).isNotNull();
            verify(savedQueryRepository).save(argThat(query -> 
                query.getParameterDefinitions() == null
            ));
        }

        @Test
        @DisplayName("非常に長いSQL文を処理する")
        void shouldHandleVeryLongSqlContent() {
            // Given
            String longSqlContent = "SELECT * FROM users WHERE ".repeat(1000) + "id = 1";
            SavedQuery expectedQuery = new SavedQuery();
            
            when(savedQueryRepository.existsByUserAndName(testUser, testQueryName)).thenReturn(false);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(expectedQuery);

            // When
            SavedQuery result = queryManagementService.saveQuery(
                testQueryName, longSqlContent, testDescription, null,
                SavedQuery.SharingScope.PRIVATE, testUser, testConnection
            );

            // Then
            assertThat(result).isNotNull();
            verify(savedQueryRepository).save(argThat(query ->
                query.getSqlContent().equals(longSqlContent)
            ));
        }

        @Test
        @DisplayName("特殊文字を含むクエリ名を処理する")
        void shouldHandleSpecialCharactersInQueryName() {
            // Given
            String specialQueryName = "Test Query @#$%^&*()_+{}[]|\\:;\"'<>?,./";
            SavedQuery expectedQuery = new SavedQuery();
            
            when(savedQueryRepository.existsByUserAndName(testUser, specialQueryName)).thenReturn(false);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(expectedQuery);

            // When
            SavedQuery result = queryManagementService.saveQuery(
                specialQueryName, testSqlContent, testDescription, null,
                SavedQuery.SharingScope.PRIVATE, testUser, testConnection
            );

            // Then
            assertThat(result).isNotNull();
            verify(savedQueryRepository).save(argThat(query ->
                query.getName().equals(specialQueryName)
            ));
        }

        @Test
        @DisplayName("大量のパラメータ定義を処理する")
        void shouldHandleLargeParameterDefinitions() {
            // Given
            Map<String, String> largeParameterDefinitions = Map.of(
                "param1", "INTEGER", "param2", "VARCHAR", "param3", "BOOLEAN",
                "param4", "DECIMAL", "param5", "DATE", "param6", "TIMESTAMP",
                "param7", "TEXT", "param8", "BIGINT", "param9", "FLOAT",
                "param10", "CHAR"
            );
            SavedQuery expectedQuery = new SavedQuery();
            
            when(savedQueryRepository.existsByUserAndName(testUser, testQueryName)).thenReturn(false);
            when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(expectedQuery);

            // When
            SavedQuery result = queryManagementService.saveQuery(
                testQueryName, testSqlContent, testDescription, largeParameterDefinitions,
                SavedQuery.SharingScope.PRIVATE, testUser, testConnection
            );

            // Then
            assertThat(result).isNotNull();
            verify(savedQueryRepository).save(any(SavedQuery.class));
        }
    }
}