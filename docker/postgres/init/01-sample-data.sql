-- PostgreSQL用サンプルデータ初期化スクリプト

-- サンプルテーブル: users
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- サンプルテーブル: products
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    stock_quantity INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 商品カテゴリのENUM型
CREATE TYPE order_status AS ENUM ('pending', 'processing', 'shipped', 'delivered', 'cancelled');

-- サンプルテーブル: orders
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status order_status DEFAULT 'pending',
    order_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 更新日時自動更新のトリガー関数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- usersテーブルの更新日時トリガー
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- インデックス作成
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- サンプルデータ挿入
INSERT INTO users (username, email, full_name) VALUES
('john_doe', 'john@example.com', 'John Doe'),
('jane_smith', 'jane@example.com', 'Jane Smith'),
('admin_user', 'admin@example.com', 'Admin User')
ON CONFLICT (username) DO NOTHING;

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