# SqlApp2 開発環境構築ガイド

## プロジェクト概要

**SqlApp2** は本格運用対応のエンタープライズ級SQL実行ツールです。

### 技術スタック
- **Backend**: Java 21 + Spring Boot 3.5.4, Spring Security, JPA/Hibernate
- **Frontend**: React 18 + TypeScript, Vite 7.1.1, React Router, react-i18next v15.6.1
- **Database**: H2 (内部) + MySQL/PostgreSQL/MariaDB (外部接続)
- **Deployment**: Docker + Docker Compose, GitHub Actions CI/CD
- **Monitoring**: Prometheus + Grafana + Spring Boot Actuator

## 前提条件

### 必須ソフトウェア
- **Java 21** (OpenJDK推奨)
- **Node.js 18+** (npm含む)
- **Git**

### 推奨ソフトウェア
- **Docker & Docker Compose** (コンテナ開発・本番デプロイ用)
- **IDE**: IntelliJ IDEA Ultimate, VS Code with Java/TypeScript extensions

## プロジェクト構造

```
sqlapp2/
├── src/main/java/cherry/sqlapp2/   # Spring Boot アプリケーション
│   ├── controller/                 # REST API エンドポイント
│   ├── service/                   # ビジネスロジック層
│   ├── entity/                    # JPA エンティティ
│   ├── dto/                       # データ転送オブジェクト
│   ├── repository/                # Spring Data JPA リポジトリ
│   └── config/                    # 設定クラス (Security, JWT等)
├── src/main/resources/
│   ├── application*.properties    # 環境別設定ファイル
│   └── static/                    # ビルド済みフロントエンド資産
├── frontend/                      # React + TypeScript アプリケーション
│   ├── src/components/           # React コンポーネント (*Page.tsx)
│   ├── src/locales/             # 国際化翻訳ファイル (en/, ja/)
│   └── src/context/             # React Context (認証等)
├── monitoring/                   # Prometheus/Grafana 設定
├── .github/workflows/           # GitHub Actions CI/CD
├── build.gradle                 # Gradle ビルド設定
├── Dockerfile                   # 本番用 Docker イメージ
└── docker-compose*.yml         # Docker Compose 設定
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

### 認証 API
- `POST /api/auth/register` - ユーザー登録
- `POST /api/auth/login` - ログイン (JWT トークン取得)
- `POST /api/auth/refresh` - リフレッシュトークン更新
- `POST /api/auth/logout` - ログアウト
- `GET /api/auth/me` - 現在ユーザー情報取得

### データベース接続 API
- `GET /api/connections` - 接続一覧取得
- `POST /api/connections` - 新規接続作成
- `PUT /api/connections/{id}` - 接続更新
- `DELETE /api/connections/{id}` - 接続削除
- `POST /api/connections/{id}/test` - 接続テスト

### SQL実行 API
- `POST /api/sql/execute` - SQL実行
- `POST /api/sql/validate` - SQL検証

### クエリ管理 API
- `GET /api/queries/saved` - 保存済みクエリ一覧
- `POST /api/queries/save` - クエリ保存
- `GET /api/queries/history` - 実行履歴

### スキーマ情報 API
- `GET /api/schema/{connectionId}` - データベーススキーマ情報

### システム API
- `GET /api/health` - アプリケーションヘルスチェック
- `GET /api/swagger-ui.html` - API ドキュメント (OpenAPI/Swagger)
- `GET /actuator/*` - Spring Boot Actuator エンドポイント

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

**現在のテスト実績**: 356テスト (303単体 + 53統合) - 100%成功率

```bash
# 全バックエンドテスト (単体 + 統合テスト)
./gradlew test

# フロントエンドテスト (vitest)
cd frontend
npm test

# ビルド確認 (全テスト含む)
./gradlew build
```

## 本番デプロイ

### 単体 WAR デプロイ
```bash
./gradlew build
java -jar build/libs/sqlapp2-1.0.0.war --server.port=8080
```

### Docker デプロイ (推奨)
```bash
# 開発環境
docker-compose up -d

# 本番環境 (監視込み)
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

### 環境設定
- **dev**: 開発環境 (H2 in-memory)
- **staging**: ステージング環境 (H2 file)
- **prod**: 本番環境 (H2 file + 構造化ログ + メトリクス)

## 監視・運用

### Spring Boot Actuator
- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Info**: `GET /actuator/info`
- **Prometheus**: `GET /actuator/prometheus`

### 監視スタック (Docker Compose)
- **Prometheus**: メトリクス収集 (http://localhost:9090)
- **Grafana**: ダッシュボード (http://localhost:3000)
- **AlertManager**: アラート通知 (http://localhost:9093)

## 参考資料

### 技術ドキュメント
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React Documentation](https://react.dev)
- [Vite Documentation](https://vitejs.dev)
- [Docker Documentation](https://docs.docker.com)

### プロジェクト固有
- **CLAUDE.md**: 開発ガイドライン・アーキテクチャ詳細
- **ROADMAP.md**: プロジェクト進捗・完成状況
- **CONTRIBUTING.md**: コントリビューションガイド
- **Swagger UI**: API仕様書 (http://localhost:8080/api/swagger-ui.html)