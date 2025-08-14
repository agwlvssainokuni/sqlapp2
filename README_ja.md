# SqlApp2

**エンタープライズ級Webベース SQL実行ツール**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

SqlApp2は、複数のデータベースシステムに対してSQL クエリを実行するための直感的なインターフェースを提供する、本格運用対応の安全なWebアプリケーションです。エンタープライズ級のセキュリティ、包括的なテスト（356テスト・100%成功率）、そして最新の技術スタックで構築されています。

## ✨ 主要機能

### 🔐 エンタープライズセキュリティ
- **JWT認証** - リフレッシュトークン・セキュアセッション管理
- **AES-256-GCM暗号化** - データベースパスワード・機密データの暗号化
- **SQLインジェクション対策** - PreparedStatementベースのパラメータ化クエリ
- **OWASP準拠** - 包括的セキュリティテスト・脆弱性スキャン

### 🗄️ マルチデータベース対応
- **MySQL, PostgreSQL, MariaDB** - 最適化ドライバによる完全互換性
- **接続管理** - 暗号化パスワードによるセキュア保存
- **リアルタイム接続テスト** - クエリ実行前の接続確認
- **ユーザー分離** - ユーザー別データベース接続の完全分離

### 📊 高度なSQL機能
- **直接SQL実行** - 高度なパラメータ処理
- **ビジュアルクエリビルダー** - スキーマ対応ドラッグ&ドロップインターフェース
- **パラメータ化クエリ** - 名前付きパラメータ（`:param`）サポート・型変換
- **クエリ管理** - SQL クエリの保存・共有・履歴追跡による整理
- **スキーマブラウジング** - データベーステーブル・カラム・メタデータ探索

### 🌐 モダンユーザー体験
- **国際化対応** - 英語・日本語完全対応インターフェース・リアルタイム切替
- **レスポンシブデザイン** - モバイルファースト・全デバイス最適化
- **リアルタイムバリデーション** - SQL構文チェック・パラメータ検出
- **パフォーマンス追跡** - クエリ実行履歴・タイミング・分析

## 🚀 クイックスタート

### オプション1: Dockerデプロイ（推奨）

```bash
git clone https://github.com/your-username/sqlapp2.git
cd sqlapp2
docker-compose up -d
```

アプリケーションに **http://localhost:8080** でアクセス

### オプション2: ローカル開発

```bash
# クローン&ビルド
git clone https://github.com/your-username/sqlapp2.git
cd sqlapp2
./gradlew build

# アプリケーション実行
java -jar build/libs/sqlapp2-1.0.0.war

# http://localhost:8080 でアクセス
```

### オプション3: 開発モード

```bash
# ターミナル1: バックエンド（ホットリロード）
./gradlew bootRun

# ターミナル2: フロントエンド（ホットリロード）
cd frontend && npm install && npm run dev

# バックエンド: http://localhost:8080
# フロントエンド: http://localhost:5173
```

## 🛠️ 技術スタック

### バックエンド
- **Java 21** - 最新LTS・モダン言語機能
- **Spring Boot 3.5.4** - 本格運用対応アプリケーションフレームワーク
- **Spring Security + JWT** - エンタープライズ認証・認可
- **JPA/Hibernate** - H2内部データベースによるデータベース抽象化
- **包括的テスト** - 356テスト（303単体 + 53統合）・100%成功率

### フロントエンド
- **React 19** - モダンコンポーネントベースUI ライブラリ
- **TypeScript** - 型安全JavaScript開発
- **Vite 7.1.1** - 高速開発ビルドツール
- **react-i18next v15.6.1** - 590+翻訳キーによる国際化
- **レスポンシブCSS3** - Grid/Flexboxによるモバイルファーストデザイン

### 運用・監視
- **Docker + Docker Compose** - 監視スタック付きコンテナデプロイ
- **GitHub Actions CI/CD** - 自動テスト・セキュリティスキャン・デプロイ
- **Prometheus + Grafana** - メトリクス収集・監視・アラート
- **OpenAPI/Swagger** - インタラクティブAPI ドキュメント

## 📖 ユーザーガイド

### 開始手順
1. **登録・ログイン**: アカウント作成・JWT認証取得
2. **データベース設定**: 暗号化パスワード保存によるデータベース接続追加
3. **SQL実行**: パラメータサポート付きクエリ記述・実行
4. **クエリ管理**: 頻繁に使用するクエリの保存・実行履歴参照
5. **ビジュアルビルダー**: ドラッグ&ドロップインターフェースによるクエリ作成

### 主要ワークフロー

**パラメータ化クエリ:**
```sql
SELECT * FROM users 
WHERE age > :minAge AND department = :dept AND created_date > :startDate
```
SqlApp2は自動的にパラメータを検出し、型に適したな入力フィールドを提供します。

**ビジュアルクエリ構築:**
- スキーマブラウザからテーブル・カラム選択
- ビジュアル演算子による条件構築
- バリデーション付きリアルタイムSQL生成
- ワンクリック実行・結果表示

### データベース接続
以下への安全な接続をサポート:
- **MySQL** (5.7+)
- **PostgreSQL** (10+)
- **MariaDB** (10+)

接続詳細はAES-256-GCMで暗号化され、安全に保存されます。

## 🔧 設定

### 環境変数
```bash
# アプリケーション設定
SERVER_PORT=8080
PROFILE=prod                    # dev, staging, prod

# JWT設定
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400            # 24時間
JWT_REFRESH_EXPIRATION=604800   # 7日間

# データベース設定（内部H2）
DB_MODE=file                    # memory, file, server
DB_PATH=/app/data/sqlapp2       # fileモード用

# 監視（オプション）
METRICS_ENABLED=true
PROMETHEUS_ENABLED=true
```

### 本番デプロイ
```bash
# 監視スタック付き
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# 環境固有設定
export SPRING_PROFILES_ACTIVE=prod
java -jar sqlapp2-1.0.0.war
```

## 🧪 品質保証

**テストカバレッジ**: 356テスト・100%成功率
- **単体テスト（303）**: サービス層・セキュリティコンポーネント・ユーティリティ
- **統合テスト（53）**: REST APIエンドポイント・データベース統合
- **セキュリティテスト**: JWT検証・暗号化・SQLインジェクション防止
- **パフォーマンステスト**: クエリ実行タイミング・最適化

**セキュリティ標準**: OWASP準拠・定期脆弱性スキャン
**コード品質**: SonarCloud統合・包括的分析

## 🔗 API ドキュメント

インタラクティブAPI ドキュメントは以下で利用可能:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **ヘルスチェック**: http://localhost:8080/actuator/health
- **メトリクス**: http://localhost:8080/actuator/metrics

## 🤝 コントリビューション

コントリビューションを歓迎します！以下のガイドをご参照ください:
- **[コントリビューションガイド](CONTRIBUTING.md)** - コントリビューションプロセス・コーディング規約
- **[技術ガイド](CLAUDE.md)** - アーキテクチャ詳細・実装ガイドライン

## 📚 ドキュメント

- **[ROADMAP.md](ROADMAP.md)** - プロジェクト進捗・完了状況
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - コントリビューションガイドライン・規約
- **[CLAUDE.md](CLAUDE.md)** - 技術アーキテクチャ・開発ガイダンス

## 🐛 サポート

- **Issues**: [GitHub Issues](https://github.com/your-username/sqlapp2/issues)
- **ディスカッション**: [GitHub Discussions](https://github.com/your-username/sqlapp2/discussions)
- **API ドキュメント**: [Swagger UI](http://localhost:8080/api/swagger-ui.html)

## 📜 ライセンス

このプロジェクトはApache License 2.0の下でライセンスされています - 詳細は[LICENSE](LICENSE)ファイルをご覧ください。

## 🏆 プロジェクト状況

**ステータス**: ✅ **本格運用対応** - エンタープライズ級SQL実行ツール

**主要成果**:
- 🎯 33開発フェーズによる完全機能実装
- 🧪 信頼性確保のための356テスト・100%成功率
- 🔒 OWASP準拠・包括的暗号化によるエンタープライズセキュリティ
- 🌐 完全国際化サポート（英語・日本語）
- 📊 Prometheus/Grafana による高度監視・可観測性
- 🚀 GitHub Actions による完全CI/CD自動化

---

**SqlApp2** - モダン・セキュア・エンタープライズ級SQLツールによるデータベース専門家支援

*Java 21・Spring Boot 3.5.4・React 19で❤️を込めて作成*