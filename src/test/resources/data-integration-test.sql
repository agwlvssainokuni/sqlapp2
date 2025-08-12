-- Integration Test Data Setup for SqlApp2
-- Apache License, Version 2.0

-- Test Users (password is 'password123' hashed with BCrypt)
INSERT INTO users (id, username, password, email, created_at, updated_at) VALUES
(1, 'testuser1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'testuser1@example.com', '2024-01-01 10:00:00', '2024-01-01 10:00:00'),
(2, 'testuser2', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'testuser2@example.com', '2024-01-01 11:00:00', '2024-01-01 11:00:00'),
(3, 'testuser3', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'testuser3@example.com', '2024-01-01 12:00:00', '2024-01-01 12:00:00');

-- Test Database Connections (encrypted passwords using test key)
INSERT INTO database_connections (id, user_id, name, database_type, host, port, database_name, username, password_encrypted, created_at, updated_at) VALUES
(1, 1, 'Test MySQL Connection', 'MYSQL', 'localhost', 3306, 'testdb', 'testuser', 'encrypted_test_password_1', '2024-01-01 10:30:00', '2024-01-01 10:30:00'),
(2, 1, 'Test PostgreSQL Connection', 'POSTGRESQL', 'localhost', 5432, 'testdb', 'testuser', 'encrypted_test_password_2', '2024-01-01 10:45:00', '2024-01-01 10:45:00'),
(3, 2, 'Test MariaDB Connection', 'MARIADB', 'localhost', 3306, 'testdb', 'testuser', 'encrypted_test_password_3', '2024-01-01 11:30:00', '2024-01-01 11:30:00');

-- Test Saved Queries
INSERT INTO saved_queries (id, user_id, name, description, sql_content, sharing_scope, execution_count, last_executed_at, created_at, updated_at) VALUES
(1, 1, 'Select All Users', 'Simple query to select all users', 'SELECT * FROM users', 'PRIVATE', 5, '2024-01-02 09:00:00', '2024-01-01 15:00:00', '2024-01-02 09:00:00'),
(2, 1, 'User Count by Date', 'Count users by registration date', 'SELECT DATE(created_at) as reg_date, COUNT(*) as user_count FROM users WHERE created_at >= :startDate GROUP BY DATE(created_at)', 'PUBLIC', 3, '2024-01-02 10:00:00', '2024-01-01 16:00:00', '2024-01-02 10:00:00'),
(3, 2, 'Find User by Username', 'Find user by username parameter', 'SELECT id, username, email FROM users WHERE username = :username', 'PRIVATE', 2, '2024-01-02 11:00:00', '2024-01-01 17:00:00', '2024-01-02 11:00:00'),
(4, 1, 'Public Query Example', 'Example of a public shared query', 'SELECT COUNT(*) as total_users FROM users', 'PUBLIC', 1, '2024-01-02 12:00:00', '2024-01-01 18:00:00', '2024-01-02 12:00:00');

-- Test Query Parameters
INSERT INTO query_parameters (id, saved_query_id, parameter_name, parameter_type, default_value, is_required, description, created_at) VALUES
(1, 2, 'startDate', 'DATE', '2024-01-01', true, 'Start date for user registration filtering', '2024-01-01 16:15:00'),
(2, 3, 'username', 'STRING', 'testuser1', true, 'Username to search for', '2024-01-01 17:15:00');

-- Test Query Execution History
INSERT INTO query_history (id, user_id, saved_query_id, connection_id, sql_content, parameter_values, execution_time, record_count, is_successful, error_message, executed_at) VALUES
(1, 1, 1, 1, 'SELECT * FROM users', NULL, 150, 3, true, NULL, '2024-01-02 09:00:00'),
(2, 1, 2, 1, 'SELECT DATE(created_at) as reg_date, COUNT(*) as user_count FROM users WHERE created_at >= ? GROUP BY DATE(created_at)', '{"startDate":"2024-01-01"}', 200, 1, true, NULL, '2024-01-02 10:00:00'),
(3, 2, 3, 3, 'SELECT id, username, email FROM users WHERE username = ?', '{"username":"testuser1"}', 120, 1, true, NULL, '2024-01-02 11:00:00'),
(4, 1, NULL, 1, 'SELECT COUNT(*) FROM users', NULL, 80, 1, true, NULL, '2024-01-02 13:00:00'),
(5, 2, NULL, 3, 'INVALID SQL QUERY', NULL, 50, 0, false, 'SQL syntax error near INVALID', '2024-01-02 14:00:00');

-- Note: Using auto-increment for primary keys, starting from higher values to avoid conflicts with test data