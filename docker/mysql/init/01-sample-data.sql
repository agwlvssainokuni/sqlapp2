-- MySQL用サンプルデータ初期化スクリプト

-- サンプルテーブル: users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- サンプルテーブル: products
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    stock_quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- サンプルテーブル: orders
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('pending', 'processing', 'shipped', 'delivered', 'cancelled') DEFAULT 'pending',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- サンプルデータ挿入
INSERT INTO users (username, email, full_name) VALUES
('john_doe', 'john@example.com', 'John Doe'),
('jane_smith', 'jane@example.com', 'Jane Smith'),
('admin_user', 'admin@example.com', 'Admin User');

INSERT INTO products (name, description, price, category, stock_quantity) VALUES
('ノートパソコン', '高性能ビジネス向けノートPC', 89800.00, 'Electronics', 15),
('マウス', 'ワイヤレス光学マウス', 2980.00, 'Electronics', 50),
('キーボード', 'メカニカルキーボード', 12800.00, 'Electronics', 25),
('コーヒー豆', 'プレミアムブレンド', 1200.00, 'Food', 100),
('書籍', 'プログラミング入門', 3800.00, 'Books', 30);

INSERT INTO orders (user_id, total_amount, status) VALUES
(1, 92780.00, 'delivered'),
(2, 16600.00, 'processing'),
(1, 1200.00, 'shipped');