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

import cherry.sqlapp2.dto.SqlExecutionResult;
import cherry.sqlapp2.entity.QueryHistory;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.DatabaseConnectionRepository;
import cherry.sqlapp2.enums.DatabaseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqlExecutionService - SQL実行サービス")
class SqlExecutionServiceTest {

    @Mock
    private DynamicDataSourceService dataSourceService;

    @Mock
    private QueryManagementService queryManagementService;

    @Mock
    private DatabaseConnectionRepository connectionRepository;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData metaData;

    private SqlExecutionService sqlExecutionService;

    private final String testUsername = "testUser";
    private final String testPassword = "hashedPassword123";
    private final String testEmail = "test@example.com";
    private final Long testConnectionId = 1L;
    private final String testSql = "SELECT * FROM users";

    private User testUser;
    private cherry.sqlapp2.entity.DatabaseConnection testConnectionEntity;
    private SavedQuery testSavedQuery;
    private QueryHistory testQueryHistory;

    @BeforeEach
    void setUp() {
        sqlExecutionService = new SqlExecutionService(
                dataSourceService, queryManagementService, connectionRepository
        );

        testUser = new User(testUsername, testPassword, testEmail);
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testConnectionEntity = new cherry.sqlapp2.entity.DatabaseConnection(
                testUser, "Test Connection", DatabaseType.MYSQL, "localhost", 3306, "testdb", "dbuser"
        );
        ReflectionTestUtils.setField(testConnectionEntity, "id", testConnectionId);

        testSavedQuery = new SavedQuery();
        testSavedQuery.setName("Test Query");
        testSavedQuery.setSqlContent(testSql);
        testSavedQuery.setUser(testUser);
        ReflectionTestUtils.setField(testSavedQuery, "id", 1L);

        testQueryHistory = new QueryHistory();
        testQueryHistory.setSqlContent(testSql);
        testQueryHistory.setUser(testUser);
        testQueryHistory.setConnection(testConnectionEntity);
        ReflectionTestUtils.setField(testQueryHistory, "id", 1L);
    }

    @Nested
    @DisplayName("SQL検証")
    class SqlValidation {

        @Test
        @DisplayName("有効なSQL文を正常に検証する")
        void shouldValidateValidSqlSuccessfully() {
            // Given
            String validSql = "SELECT * FROM users WHERE id = 1";

            // When & Then
            assertThatNoException().isThrownBy(() -> sqlExecutionService.validateQuery(validSql));
        }

        @Test
        @DisplayName("危険なDROPDATABASE操作を検出する")
        void shouldDetectDangerousDropDatabaseOperation() {
            // Given
            String dangerousSql = "DROP DATABASE testdb";

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.validateQuery(dangerousSql))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Potentially dangerous SQL operation detected: drop database");
        }

        @Test
        @DisplayName("危険なDROPTABLE操作を検出する")
        void shouldDetectDangerousDropTableOperation() {
            // Given
            String dangerousSql = "DROP TABLE users";

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.validateQuery(dangerousSql))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Potentially dangerous SQL operation detected: drop table");
        }

        @Test
        @DisplayName("危険なTRUNCATE操作を検出する")
        void shouldDetectDangerousTruncateOperation() {
            // Given
            String dangerousSql = "TRUNCATE users";

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.validateQuery(dangerousSql))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Potentially dangerous SQL operation detected: truncate");
        }

        @Test
        @DisplayName("危険なALTER操作を検出する")
        void shouldDetectDangerousAlterOperation() {
            // Given
            String dangerousSql = "ALTER TABLE users ADD COLUMN new_col VARCHAR(50)";

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.validateQuery(dangerousSql))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Potentially dangerous SQL operation detected: alter table");
        }

        @Test
        @DisplayName("危険なGRANT操作を検出する")
        void shouldDetectDangerousGrantOperation() {
            // Given
            String dangerousSql = "GRANT ALL PRIVILEGES ON *.* TO 'user'@'localhost'";

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.validateQuery(dangerousSql))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Potentially dangerous SQL operation detected: grant");
        }

        @Test
        @DisplayName("長すぎるSQL文を検出する")
        void shouldDetectTooLongSqlQuery() {
            // Given
            String longSql = "SELECT * FROM users WHERE ".repeat(1000) + "id = 1";

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.validateQuery(longSql))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("SQL query too long (maximum 10,000 characters)");
        }

        @Test
        @DisplayName("大文字・小文字混在の危険操作を検出する")
        void shouldDetectMixedCaseDangerousOperations() {
            // Given
            String dangerousSql = "Drop Table users";

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.validateQuery(dangerousSql))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Potentially dangerous SQL operation detected: drop table");
        }
    }

    @Nested
    @DisplayName("通常SQL実行")
    class RegularSqlExecution {

        @Test
        @DisplayName("SELECT文を正常に実行する")
        void shouldExecuteSelectQuerySuccessfully() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(2, Arrays.asList("id", "name"), Arrays.asList(
                    Arrays.asList(1, "John"),
                    Arrays.asList(2, "Jane")
            ));

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().rowCount()).isEqualTo(2);
            assertThat(result.data().columns()).containsExactly("id", "name");

            verify(dataSourceService).getConnection(testUser, testConnectionId);
            verify(statement).executeQuery(testSql);
            verify(queryManagementService).recordExecution(
                    any(String.class), any(), anyLong(), any(Integer.class), any(Boolean.class), any(), 
                    any(User.class), any(cherry.sqlapp2.entity.DatabaseConnection.class), any()
            );
        }

        @Test
        @DisplayName("UPDATE文を正常に実行する")
        void shouldExecuteUpdateQuerySuccessfully() throws Exception {
            // Given
            String updateSql = "UPDATE users SET name = 'Updated' WHERE id = 1";
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeUpdate(updateSql)).thenReturn(1);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, updateSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.data()).isNull(); // UPDATE queries don't return data

            verify(statement).executeUpdate(updateSql);
            verify(queryManagementService).recordExecution(
                    any(String.class), any(), anyLong(), any(), any(Boolean.class), any(), 
                    any(User.class), any(cherry.sqlapp2.entity.DatabaseConnection.class), any()
            );
        }

        @Test
        @DisplayName("WITH句を含むSELECT文を実行する")
        void shouldExecuteWithQuerySuccessfully() throws Exception {
            // Given
            String withSql = "WITH cte AS (SELECT * FROM users) SELECT * FROM cte";
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(withSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(1, Arrays.asList("id"), Arrays.asList(Arrays.asList(1)));

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, withSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            verify(statement).executeQuery(withSql);
        }

        @Test
        @DisplayName("SavedQueryとの関連付けでSQL実行する")
        void shouldExecuteQueryWithSavedQueryAssociation() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(1, Arrays.asList("count"), Arrays.asList(Arrays.asList(5)));

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, testSavedQuery);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.savedQueryId()).isEqualTo(testSavedQuery.getId());
            
            verify(queryManagementService).recordExecution(
                    any(String.class), any(), anyLong(), any(Integer.class), any(Boolean.class), any(), 
                    any(User.class), any(cherry.sqlapp2.entity.DatabaseConnection.class), any(SavedQuery.class)
            );
        }

        @Test
        @DisplayName("SQL実行エラーを適切に処理する")
        void shouldHandleSqlExecutionError() throws Exception {
            // Given
            SQLException sqlException = new SQLException("Table not found", "42S02", 1146);
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenThrow(sqlException);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isFalse();
            assertThat(result.error()).isEqualTo("Table not found");
            assertThat(result.errorType()).isEqualTo("SQLException");
            assertThat(result.errorCode()).isEqualTo(1146);
            assertThat(result.sqlState()).isEqualTo("42S02");

            verify(queryManagementService).recordExecution(
                    any(String.class), any(), anyLong(), any(), any(Boolean.class), any(String.class), 
                    any(User.class), any(cherry.sqlapp2.entity.DatabaseConnection.class), any()
            );
        }
    }

    @Nested
    @DisplayName("パラメータ化SQL実行")
    class ParameterizedSqlExecution {

        @Test
        @DisplayName("名前付きパラメータでSELECT文を実行する")
        void shouldExecuteParameterizedSelectQuery() throws Exception {
            // Given
            String paramSql = "SELECT * FROM users WHERE id = :userId AND name LIKE :userName";
            Map<String, Object> parameters = Map.of("userId", 1, "userName", "John%");
            Map<String, String> parameterTypes = Map.of("userId", "int", "userName", "string");

            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.prepareStatement("SELECT * FROM users WHERE id = ? AND name LIKE ?")).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(1, Arrays.asList("id", "name"), Arrays.asList(Arrays.asList(1, "John")));

            // When
            SqlExecutionResult result = sqlExecutionService.executeParameterizedQuery(
                    testUser, testConnectionId, paramSql, parameters, parameterTypes, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.data().rowCount()).isEqualTo(1);

            verify(preparedStatement).setInt(1, 1);
            verify(preparedStatement).setString(2, "John%");
            verify(preparedStatement).executeQuery();
            verify(queryManagementService).recordExecution(
                    any(String.class), any(), anyLong(), any(Integer.class), any(Boolean.class), any(), 
                    any(User.class), any(cherry.sqlapp2.entity.DatabaseConnection.class), any()
            );
        }

        @Test
        @DisplayName("様々なデータ型のパラメータを処理する")
        void shouldHandleVariousParameterTypes() throws Exception {
            // Given
            String paramSql = "INSERT INTO test_table VALUES (:intVal, :longVal, :doubleVal, :boolVal, :dateVal, :timeVal, :datetimeVal, :decimalVal)";
            Map<String, Object> parameters = Map.of(
                    "intVal", 123,
                    "longVal", 456L,
                    "doubleVal", 78.9,
                    "boolVal", true,
                    "dateVal", LocalDate.of(2025, 1, 15),
                    "timeVal", LocalTime.of(14, 30, 0),
                    "datetimeVal", LocalDateTime.of(2025, 1, 15, 14, 30, 0),
                    "decimalVal", new BigDecimal("123.45")
            );
            Map<String, String> parameterTypes = Map.of(
                    "intVal", "int",
                    "longVal", "long", 
                    "doubleVal", "double",
                    "boolVal", "boolean",
                    "dateVal", "date",
                    "timeVal", "time", 
                    "datetimeVal", "datetime",
                    "decimalVal", "decimal"
            );

            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            // When
            SqlExecutionResult result = sqlExecutionService.executeParameterizedQuery(
                    testUser, testConnectionId, paramSql, parameters, parameterTypes, null);

            // Then
            assertThat(result.ok()).isTrue();

            verify(preparedStatement).setInt(anyInt(), eq(123));
            verify(preparedStatement).setLong(anyInt(), eq(456L));
            verify(preparedStatement).setDouble(anyInt(), eq(78.9));
            verify(preparedStatement).setBoolean(anyInt(), eq(true));
            verify(preparedStatement).setDate(anyInt(), eq(java.sql.Date.valueOf(LocalDate.of(2025, 1, 15))));
            verify(preparedStatement).setTime(anyInt(), eq(Time.valueOf(LocalTime.of(14, 30, 0))));
            verify(preparedStatement).setTimestamp(anyInt(), eq(Timestamp.valueOf(LocalDateTime.of(2025, 1, 15, 14, 30, 0))));
            verify(preparedStatement).setBigDecimal(anyInt(), eq(new BigDecimal("123.45")));
        }

        @Test
        @DisplayName("自動型検出でパラメータを処理する")
        void shouldHandleAutoTypeDetectionForParameters() throws Exception {
            // Given
            String paramSql = "SELECT * FROM users WHERE id = :userId AND name = :userName";
            Map<String, Object> parameters = Map.of("userId", 1, "userName", "John");
            Map<String, String> parameterTypes = null; // Auto-detection

            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(1, Arrays.asList("id", "name"), Arrays.asList(Arrays.asList(1, "John")));

            // When
            SqlExecutionResult result = sqlExecutionService.executeParameterizedQuery(
                    testUser, testConnectionId, paramSql, parameters, parameterTypes, null);

            // Then
            assertThat(result.ok()).isTrue();
            verify(preparedStatement).setInt(anyInt(), eq(1)); // Auto-detected as int
            verify(preparedStatement).setString(anyInt(), eq("John")); // Auto-detected as string
        }

        @Test
        @DisplayName("NULL値のパラメータを処理する")
        void shouldHandleNullParameters() throws Exception {
            // Given
            String paramSql = "SELECT * FROM users WHERE optional_field = :nullValue";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("nullValue", null);
            Map<String, String> parameterTypes = Map.of("nullValue", "string");

            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(0, Arrays.asList("id"), Arrays.asList());

            // When
            SqlExecutionResult result = sqlExecutionService.executeParameterizedQuery(
                    testUser, testConnectionId, paramSql, parameters, parameterTypes, null);

            // Then
            assertThat(result.ok()).isTrue();
            verify(preparedStatement).setNull(1, Types.NULL);
        }

        @Test
        @DisplayName("空のパラメータマップを処理する")
        void shouldHandleEmptyParametersMap() throws Exception {
            // Given
            Map<String, Object> emptyParameters = new HashMap<>();
            Map<String, String> emptyParameterTypes = new HashMap<>();

            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.prepareStatement(testSql)).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(1, Arrays.asList("count"), Arrays.asList(Arrays.asList(10)));

            // When
            SqlExecutionResult result = sqlExecutionService.executeParameterizedQuery(
                    testUser, testConnectionId, testSql, emptyParameters, emptyParameterTypes, null);

            // Then
            assertThat(result.ok()).isTrue();
            verify(preparedStatement).executeQuery();
        }

        @Test
        @DisplayName("不足しているパラメータでエラーを発生させる")
        void shouldThrowErrorForMissingParameters() throws Exception {
            // Given
            String paramSql = "SELECT * FROM users WHERE id = :userId AND name = :userName";
            Map<String, Object> incompleteParameters = Map.of("userId", 1); // Missing userName

            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.executeParameterizedQuery(
                    testUser, testConnectionId, paramSql, incompleteParameters, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Parameter not provided: userName");
        }
    }

    @Nested
    @DisplayName("ResultSet処理")
    class ResultSetProcessing {

        @Test
        @DisplayName("カラム詳細情報を含むResultSetを処理する")
        void shouldProcessResultSetWithColumnDetails() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            // Setup metadata for columns
            when(resultSet.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(2);
            when(metaData.getColumnName(1)).thenReturn("id");
            when(metaData.getColumnName(2)).thenReturn("name");
            when(metaData.getColumnLabel(1)).thenReturn("User ID");
            when(metaData.getColumnLabel(2)).thenReturn("User Name");
            when(metaData.getColumnTypeName(1)).thenReturn("INT");
            when(metaData.getColumnTypeName(2)).thenReturn("VARCHAR");
            when(metaData.getColumnClassName(1)).thenReturn("java.lang.Integer");
            when(metaData.getColumnClassName(2)).thenReturn("java.lang.String");
            when(metaData.isNullable(1)).thenReturn(ResultSetMetaData.columnNoNulls);
            when(metaData.isNullable(2)).thenReturn(ResultSetMetaData.columnNullable);
            when(metaData.getPrecision(1)).thenReturn(10);
            when(metaData.getPrecision(2)).thenReturn(255);
            when(metaData.getScale(1)).thenReturn(0);
            when(metaData.getScale(2)).thenReturn(0);

            // Setup result data
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getObject(1)).thenReturn(1);
            when(resultSet.getObject(2)).thenReturn("John");

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().columnDetails()).hasSize(2);

            SqlExecutionResult.ColumnDetail idColumn = result.data().columnDetails().get(0);
            assertThat(idColumn.name()).isEqualTo("id");
            assertThat(idColumn.label()).isEqualTo("User ID");
            assertThat(idColumn.type()).isEqualTo("INT");
            assertThat(idColumn.className()).isEqualTo("java.lang.Integer");
            assertThat(idColumn.nullable()).isFalse();
            assertThat(idColumn.precision()).isEqualTo(10);
            assertThat(idColumn.scale()).isEqualTo(0);

            SqlExecutionResult.ColumnDetail nameColumn = result.data().columnDetails().get(1);
            assertThat(nameColumn.name()).isEqualTo("name");
            assertThat(nameColumn.label()).isEqualTo("User Name");
            assertThat(nameColumn.type()).isEqualTo("VARCHAR");
            assertThat(nameColumn.className()).isEqualTo("java.lang.String");
            assertThat(nameColumn.nullable()).isTrue();
            assertThat(nameColumn.precision()).isEqualTo(255);
            assertThat(nameColumn.scale()).isEqualTo(0);
        }

        @Test
        @DisplayName("特殊データ型を適切に処理する")
        void shouldHandleSpecialDataTypes() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            // Mock CLOBとBLOBデータ
            Clob mockClob = mock(Clob.class);
            when(mockClob.length()).thenReturn(10L);
            when(mockClob.getSubString(1, 10)).thenReturn("text_value");

            Blob mockBlob = mock(Blob.class);

            setupMockResultSet(1, Arrays.asList("text_col", "blob_col", "date_col", "time_col", "timestamp_col"), 
                             Arrays.asList(Arrays.asList(mockClob, mockBlob, 
                                 java.sql.Date.valueOf(LocalDate.of(2025, 1, 15)),
                                 Time.valueOf(LocalTime.of(14, 30, 0)),
                                 Timestamp.valueOf(LocalDateTime.of(2025, 1, 15, 14, 30, 0)))));

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.data().rows()).hasSize(1);

            List<Object> row = result.data().rows().get(0);
            assertThat(row.get(0)).isEqualTo("text_value"); // CLOB converted to string
            assertThat(row.get(1)).isEqualTo("[BLOB data]"); // BLOB placeholder
            assertThat(row.get(2)).isInstanceOf(LocalDate.class);
            assertThat(row.get(3)).isInstanceOf(LocalTime.class);
            assertThat(row.get(4)).isInstanceOf(LocalDateTime.class);
        }

        @Test
        @DisplayName("大量データの行制限を適用する")
        void shouldApplyRowLimitForLargeDatasets() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            // Setup for 1001 rows (exceeds 1000 limit)
            when(resultSet.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(1);
            when(metaData.getColumnName(1)).thenReturn("id");
            when(metaData.getColumnLabel(1)).thenReturn("id");
            when(metaData.getColumnTypeName(1)).thenReturn("INT");
            when(metaData.getColumnClassName(1)).thenReturn("java.lang.Integer");
            when(metaData.isNullable(1)).thenReturn(ResultSetMetaData.columnNoNulls);
            when(metaData.getPrecision(1)).thenReturn(10);
            when(metaData.getScale(1)).thenReturn(0);

            Boolean[] nextResults = new Boolean[1001];
            Arrays.fill(nextResults, true);
            when(resultSet.next()).thenReturn(true, nextResults); // Always return true to simulate large dataset
            when(resultSet.getObject(1)).thenReturn(1);

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.data().rows()).hasSize(1000); // Limited to 1000 rows
            assertThat(result.data().rowCount()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("エラーハンドリングとエッジケース")
    class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("データベース接続取得失敗を処理する")
        void shouldHandleDatabaseConnectionFailure() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId))
                    .thenThrow(new SQLException("Connection failed", "08001", 2003));

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isFalse();
            assertThat(result.error()).isEqualTo("Connection failed");
            assertThat(result.errorCode()).isEqualTo(2003);
            assertThat(result.sqlState()).isEqualTo("08001");
        }

        @Test
        @DisplayName("空のResultSetを処理する")
        void shouldHandleEmptyResultSet() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(0, Arrays.asList("id", "name"), Arrays.asList()); // Empty result

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.data().rows()).isEmpty();
            assertThat(result.data().rowCount()).isEqualTo(0);
            assertThat(result.data().columns()).containsExactly("id", "name"); // Columns still present
        }

        @Test
        @DisplayName("接続が見つからない場合を処理する")
        void shouldHandleMissingConnection() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId)).thenReturn(Optional.empty());

            setupMockResultSet(1, Arrays.asList("count"), Arrays.asList(Arrays.asList(1)));

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.queryHistoryId()).isNull(); // No history recorded when connection not found
            verify(queryManagementService, never()).recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("極端に長いカラム名を処理する")
        void shouldHandleExtremelyLongColumnNames() throws Exception {
            // Given
            String longColumnName = "very_long_column_name_".repeat(20);
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(testQueryHistory);

            setupMockResultSet(1, Arrays.asList(longColumnName), Arrays.asList(Arrays.asList("value")));

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.data().columns()).containsExactly(longColumnName);
        }

        @Test
        @DisplayName("複雑なパラメータ変換エラーを処理する")
        void shouldHandleComplexParameterConversionError() throws Exception {
            // Given
            String paramSql = "SELECT * FROM users WHERE created_date = :invalidDate";
            Map<String, Object> parameters = Map.of("invalidDate", "not-a-date");
            Map<String, String> parameterTypes = Map.of("invalidDate", "date");

            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            // When & Then
            assertThatThrownBy(() -> sqlExecutionService.executeParameterizedQuery(
                    testUser, testConnectionId, paramSql, parameters, parameterTypes, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("could not be parsed");
        }

        @Test
        @DisplayName("QueryManagementService記録失敗を処理する")
        void shouldHandleQueryManagementServiceRecordingFailure() throws Exception {
            // Given
            when(dataSourceService.getConnection(testUser, testConnectionId)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(testSql)).thenReturn(resultSet);
            when(connectionRepository.findByUserAndId(testUser, testConnectionId))
                    .thenReturn(Optional.of(testConnectionEntity));
            when(queryManagementService.recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(null); // Simulate recording failure

            setupMockResultSet(1, Arrays.asList("id"), Arrays.asList(Arrays.asList(1)));

            // When
            SqlExecutionResult result = sqlExecutionService.executeQuery(testUser, testConnectionId, testSql, null);

            // Then
            assertThat(result.ok()).isTrue();
            assertThat(result.queryHistoryId()).isNull(); // No history ID when recording fails
            verify(queryManagementService).recordExecution(anyString(), any(), anyLong(), any(), anyBoolean(), any(), any(), any(), any());
        }
    }

    // Helper method to setup mock ResultSet
    private void setupMockResultSet(int expectedRowCount, List<String> columns, List<List<Object>> rows) throws SQLException {
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(columns.size());

        for (int i = 0; i < columns.size(); i++) {
            when(metaData.getColumnName(i + 1)).thenReturn(columns.get(i));
            when(metaData.getColumnLabel(i + 1)).thenReturn(columns.get(i));
            when(metaData.getColumnTypeName(i + 1)).thenReturn("VARCHAR");
            when(metaData.getColumnClassName(i + 1)).thenReturn("java.lang.String");
            when(metaData.isNullable(i + 1)).thenReturn(ResultSetMetaData.columnNullable);
            when(metaData.getPrecision(i + 1)).thenReturn(255);
            when(metaData.getScale(i + 1)).thenReturn(0);
        }

        // Setup next() calls correctly
        if (rows.isEmpty()) {
            when(resultSet.next()).thenReturn(false);
        } else if (rows.size() == 1) {
            when(resultSet.next()).thenReturn(true).thenReturn(false);
        } else if (rows.size() == 2) {
            when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        } else {
            Boolean[] nextResults = new Boolean[rows.size()];
            Arrays.fill(nextResults, 0, rows.size(), true);
            when(resultSet.next()).thenReturn(true, nextResults).thenReturn(false);
        }

        // Setup getObject calls for each row
        if (!rows.isEmpty()) {
            // For simplicity, just setup for first row data
            List<Object> firstRow = rows.get(0);
            for (int colIndex = 0; colIndex < firstRow.size(); colIndex++) {
                when(resultSet.getObject(colIndex + 1)).thenReturn(firstRow.get(colIndex));
            }
        }
    }
}