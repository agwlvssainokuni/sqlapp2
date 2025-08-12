-- Integration Test Data Setup for SqlApp2
-- Apache License, Version 2.0

-- Test Users (password is 'password123' hashed with BCrypt strength 10)
INSERT INTO users (id, username, password, email, created_at, updated_at) VALUES
(1, 'testuser1', '$2a$10$kC3jOpKyffh082s69PFsg.d964pH13k8BUUsPLi0za9VD1/XFdAVa', 'testuser1@example.com', '2024-01-01 10:00:00', '2024-01-01 10:00:00'),
(2, 'testuser2', '$2a$10$kC3jOpKyffh082s69PFsg.d964pH13k8BUUsPLi0za9VD1/XFdAVa', 'testuser2@example.com', '2024-01-01 11:00:00', '2024-01-01 11:00:00'),
(3, 'testuser3', '$2a$10$kC3jOpKyffh082s69PFsg.d964pH13k8BUUsPLi0za9VD1/XFdAVa', 'testuser3@example.com', '2024-01-01 12:00:00', '2024-01-01 12:00:00');

-- Test Database Connections (encrypted passwords using test key)
INSERT INTO database_connections (id, user_id, connection_name, database_type, host, port, database_name, username, encrypted_password, created_at, updated_at, is_active) VALUES
(1, 1, 'MySQL Connection', 'MYSQL', 'localhost', 3306, 'testdb', 'testuser', 'encrypted_test_password_1', '2024-01-01 10:30:00', '2024-01-01 10:30:00', true),
(2, 1, 'PostgreSQL Connection', 'POSTGRESQL', 'localhost', 5432, 'testdb', 'testuser', 'encrypted_test_password_2', '2024-01-01 10:45:00', '2024-01-01 10:45:00', true),
(3, 2, 'MariaDB Connection', 'MARIADB', 'localhost', 3306, 'testdb', 'testuser', 'encrypted_test_password_3', '2024-01-01 11:30:00', '2024-01-01 11:30:00', true);

-- Test Saved Queries
INSERT INTO saved_queries (id, user_id, connection_id, name, description, sql_content, sharing_scope, execution_count, last_executed_at, created_at, updated_at, parameter_definitions) VALUES
(1, 1, 1, 'Select All Users', 'Simple query to select all users', 'SELECT * FROM users', 'PRIVATE', 5, '2024-01-02 09:00:00', '2024-01-01 15:00:00', '2024-01-02 09:00:00', NULL),
(2, 1, 1, 'User Count by Date', 'Count users by registration date', 'SELECT DATE(created_at) as reg_date, COUNT(*) as user_count FROM users WHERE created_at >= :startDate GROUP BY DATE(created_at)', 'PUBLIC', 3, '2024-01-02 10:00:00', '2024-01-01 16:00:00', '2024-01-02 10:00:00', NULL),
(3, 2, 3, 'Find User by Username', 'Find user by username parameter', 'SELECT id, username, email FROM users WHERE username = :username', 'PRIVATE', 2, '2024-01-02 11:00:00', '2024-01-01 17:00:00', '2024-01-02 11:00:00', NULL),
(4, 1, 1, 'Public Query Example', 'Example of a public shared query', 'SELECT COUNT(*) as total_users FROM users', 'PUBLIC', 1, '2024-01-02 12:00:00', '2024-01-01 18:00:00', '2024-01-02 12:00:00', NULL);

-- Test Query Execution History
INSERT INTO query_history (id, user_id, saved_query_id, connection_id, sql_content, parameter_values, execution_time_ms, result_count, is_successful, error_message, executed_at, connection_name, database_type) VALUES
(1, 1, 1, 1, 'SELECT * FROM users', NULL, 150, 3, true, NULL, '2024-01-02 09:00:00', 'Test MySQL Connection', 'MYSQL'),
(2, 1, 2, 1, 'SELECT DATE(created_at) as reg_date, COUNT(*) as user_count FROM users WHERE created_at >= ? GROUP BY DATE(created_at)', '{"startDate":"2024-01-01"}', 200, 1, true, NULL, '2024-01-02 10:00:00', 'Test MySQL Connection', 'MYSQL'),
(3, 2, 3, 3, 'SELECT id, username, email FROM users WHERE username = ?', '{"username":"testuser1"}', 120, 1, true, NULL, '2024-01-02 11:00:00', 'Test MariaDB Connection', 'MARIADB'),
(4, 1, NULL, 1, 'SELECT COUNT(*) FROM users', NULL, 80, 1, true, NULL, '2024-01-02 13:00:00', 'Test MySQL Connection', 'MYSQL'),
(5, 2, NULL, 3, 'INVALID SQL QUERY', NULL, 50, 0, false, 'SQL syntax error near INVALID', '2024-01-02 14:00:00', 'Test MariaDB Connection', 'MARIADB');

-- Note: Using auto-increment for primary keys, starting from higher values to avoid conflicts with test data
ALTER TABLE users ALTER COLUMN id RESTART WITH 100;
ALTER TABLE database_connections ALTER COLUMN id RESTART WITH 100;
ALTER TABLE saved_queries ALTER COLUMN id RESTART WITH 100;
ALTER TABLE query_history ALTER COLUMN id RESTART WITH 100;
