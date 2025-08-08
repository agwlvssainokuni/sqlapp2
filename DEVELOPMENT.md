# SqlApp2 開発環境構築ガイド

## 前提条件

### 必須ソフトウェア
- **Java 21** (OpenJDK推奨)
- **Node.js 18+** (npm含む)
- **Git**

### 推奨ソフトウェア
- **Docker** (コンテナデプロイ用)
- **Docker Compose** (開発環境用)
- **IDE**: IntelliJ IDEA, VS Code, Eclipse等

## プロジェクト構造

```
sqlapp2/
├── src/main/java/           # Javaソースコード (Spring Boot)
├── src/main/resources/      # アプリケーションリソース
├── frontend/                # Reactフロントエンド (Vite)
├── build.gradle             # Gradleビルド設定
├── Dockerfile              # Dockerイメージ定義
├── docker-compose.yml      # Docker Compose設定
└── DEVELOPMENT.md          # このファイル
```

## 開発環境セットアップ

### 1. リポジトリクローン

```bash
git clone <repository-url>
cd sqlapp2
```

### 2. バックエンド開発環境

#### Gradle Wrapperの実行権限設定
```bash
chmod +x gradlew
```

#### 依存関係の確認とビルド
```bash
./gradlew build
```

#### 開発サーバー起動
```bash
./gradlew bootRun
```

バックエンドは `http://localhost:8080` で起動します。

### 3. フロントエンド開発環境

#### 依存関係インストール
```bash
cd frontend
npm install
```

#### 開発サーバー起動
```bash
npm run dev
```

フロントエンドは `http://localhost:5173` で起動します。
APIは自動的に `http://localhost:8080` にプロキシされます。

## 開発ワークフロー

### 1. 通常の開発フロー

1. **バックエンド開発**:
   ```bash
   ./gradlew bootRun
   ```

2. **フロントエンド開発** (別ターミナル):
   ```bash
   cd frontend
   npm run dev
   ```

3. **ブラウザでアクセス**: `http://localhost:5173`

### 2. 統合テスト

フロントエンドをビルドしてバックエンドに統合:

```bash
./gradlew build
./gradlew bootRun
```

統合版は `http://localhost:8080` でアクセス可能。

## Docker環境

### 1. Dockerビルド

```bash
# アプリケーションビルド
./gradlew build

# Dockerイメージビルド
docker build -t sqlapp2 .
```

### 2. Docker Compose

```bash
# サービス起動
docker-compose up -d

# ログ確認
docker-compose logs -f

# サービス停止
docker-compose down
```

## データベース

### 開発環境 (H2 in-memory)
- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:sqlapp2db`
- **Username**: `sa`
- **Password**: (空)

### 本番環境 (H2 file)
- **データファイル**: `/app/data/sqlapp2db`
- **永続化**: Dockerボリューム使用

## API エンドポイント

### 認証API
- `POST /api/auth/register` - ユーザー登録
- `POST /api/auth/login` - ログイン
- `GET /api/auth/user/{username}` - ユーザー情報取得

### システムAPI
- `GET /api/health` - ヘルスチェック

## トラブルシューティング

### よくある問題

1. **ポート競合エラー**:
   ```bash
   # プロセス確認
   lsof -i :8080
   lsof -i :5173
   
   # プロセス終了
   kill -9 <PID>
   ```

2. **npm依存関係エラー**:
   ```bash
   cd frontend
   rm -rf node_modules package-lock.json
   npm install
   ```

3. **Gradleビルドエラー**:
   ```bash
   ./gradlew clean build
   ```

4. **Docker権限エラー**:
   ```bash
   sudo docker-compose up
   ```

### ログ確認

- **Spring Boot**: コンソール出力、`app.log`
- **React**: ブラウザ開発者ツール
- **Docker**: `docker-compose logs`

## コードスタイル

### Java
- Apache License ヘッダー必須
- パッケージ名: `cherry.sqlapp2`
- Spring Boot標準規約に従う

### TypeScript/React
- Apache License ヘッダー必須
- セミコロン省略スタイル
- 関数型コンポーネント使用

## テスト実行

```bash
# バックエンドテスト
./gradlew test

# フロントエンドテスト (将来実装)
cd frontend
npm test
```

## 本番デプロイ

### WAR デプロイ
```bash
./gradlew build
java -jar build/libs/sqlapp2.war
```

### Docker デプロイ
```bash
docker-compose -f docker-compose.yml up -d
```

## 参考資料

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React Documentation](https://react.dev)
- [Vite Documentation](https://vitejs.dev)
- [Docker Documentation](https://docs.docker.com)