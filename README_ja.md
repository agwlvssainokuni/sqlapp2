# SqlApp2 🎉

**マルチRDBMS対応モダンWebベースSQLクエリツール**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

SqlApp2は、複数のデータベースシステムに対してSQLクエリを実行するための直感的なインターフェースを提供する、セキュアでモダンなWebアプリケーションです。Spring BootとReactで構築され、エンタープライズ級のセキュリティ、マルチデータベース対応、レスポンシブなユーザー体験を提供します。

## 🚀 主要機能

### 🔐 セキュリティファースト
- **JWT認証**: ステートレストークンベース認証システム
- **パスワード暗号化**: データベースパスワードのAES-256-GCM暗号化
- **SQLインジェクション対策**: PreparedStatementベースのパラメータ化クエリ
- **Spring Security統合**: エンタープライズ級セキュリティフレームワーク

### 🗄️ マルチデータベース対応
- **MySQL** - 最適化されたドライバーによる完全サポート
- **PostgreSQL** - 完全な互換性と機能
- **MariaDB** - ネイティブ統合とパフォーマンス

### 📊 SQL実行エンジン
- **パラメータ化クエリ**: 名前付きパラメータ（`:param`）サポートと自動型変換
- **リアルタイム検証**: 実行前のSQL構文チェック
- **結果表示**: メタデータ付きテーブル形式データ表示
- **パフォーマンス監視**: 実行時間追跡と統計

### 🗂️ スキーマ情報管理
- **データベーススキーマ取得**: テーブル・カラム情報の自動取得
- **メタデータ表示**: データ型・制約・サイズ情報の詳細表示
- **スキーマブラウジング**: 直感的なデータベース構造探索
- **テーブル構造確認**: SQLクエリ作成支援のための構造参照

### 📚 クエリ管理システム
- **クエリ保存・共有**: パラメータテンプレート付きでよく使用するSQLクエリを保存
- **パブリック/プライベート共有**: チームメンバーとクエリを共有するかプライベートに保持
- **クエリ履歴**: パフォーマンスメトリクス付きで全SQL実行を自動追跡
- **再実行**: パラメータ復元によるワンクリッククエリ再実行
- **パフォーマンス分析**: 実行時間追跡、成功率、使用統計

### 🎨 モダンユーザーインターフェース
- **レスポンシブデザイン**: CSS Grid/Flexboxによるモバイルファースト
- **React + TypeScript**: 型安全なコンポーネントアーキテクチャ
- **リアルタイム更新**: 動的パラメータ検出とフォーム生成
- **接続管理**: ビジュアルデータベース接続テストと管理

### 🐳 デプロイ対応
- **単一WARデプロイ**: フロントエンドとバックエンドの統合
- **Docker対応**: Docker Composeによるコンテナ化デプロイ
- **環境設定**: Twelve-Factor App準拠
- **H2内部データベース**: 設定不要の開発環境

## 📸 スクリーンショット

### ダッシュボード
![ダッシュボード](docs/images/dashboard.png)
*機能概要とナビゲーションを含むメインダッシュボード*

### 接続管理
![接続管理](docs/images/connections.png)
*データベース接続作成とテストインターフェース*

### SQL実行
![SQL実行](docs/images/sql-execution.png)
*パラメータ化クエリ対応のインタラクティブSQL実行画面*

## 🛠️ 技術スタック

### バックエンド
- **Java 21** - モダンな言語機能を持つ最新LTSバージョン
- **Spring Boot 3.5.4** - プロダクション対応フレームワーク
- **Spring Security** - 認証・認可
- **Spring Data JPA** - データベース抽象化層
- **H2 Database** - 内部データストレージ
- **Gradle 9.0.0** - ビルド自動化

### フロントエンド
- **React 18** - モダンコンポーネントベースUIライブラリ
- **TypeScript** - 型安全JavaScript開発
- **Vite 7.1.1** - 高速開発ビルドツール
- **React Router** - クライアントサイドルーティング
- **CSS3** - Grid/Flexboxによるモダンスタイリング

### データベースドライバー
- **MySQL Connector/J** - MySQL データベース接続
- **PostgreSQL JDBC Driver** - PostgreSQL統合
- **MariaDB Connector/J** - MariaDBサポート

## 🚀 クイックスタート

### 前提条件
- Java 21以降
- Node.js 18以降
- Docker（オプション）

### オプション1: Dockerデプロイ（推奨）

```bash
git clone https://github.com/your-username/sqlapp2.git
cd sqlapp2
docker-compose up -d
```

http://localhost:8080 でアプリケーションにアクセス

### オプション2: ローカル開発

1. **リポジトリのクローン**
   ```bash
   git clone https://github.com/your-username/sqlapp2.git
   cd sqlapp2
   ```

2. **ビルドと実行**
   ```bash
   ./gradlew build
   java -jar build/libs/sqlapp2-1.0.0.war
   ```

3. **アプリケーションへのアクセス**
   - ブラウザで http://localhost:8080 を開く
   - アカウントを作成またはログイン
   - データベース接続を追加
   - SQLクエリの実行開始！

### オプション3: 開発モード

ホットリロード付きのアクティブ開発用：

```bash
# ターミナル1: バックエンド開始
./gradlew bootRun

# ターミナル2: フロントエンド開始
cd frontend
npm install
npm run dev
```

- バックエンド: http://localhost:8080
- フロントエンド: http://localhost:5173（バックエンドへプロキシ）

## 📖 ユーザーガイド

### 1. ユーザー登録とログイン
1. http://localhost:8080 にアクセス
2. 「登録」をクリックして新しいアカウントを作成
3. ユーザー名、メール、パスワードを入力
4. 認証情報でログインしてJWTトークンを取得

### 2. データベース接続設定
1. ダッシュボードから「接続管理」をクリック
2. 「新しい接続を追加」をクリック
3. 接続詳細を入力：
   - **接続名**: 接続のわかりやすい名前
   - **データベースタイプ**: MySQL、PostgreSQL、またはMariaDB
   - **ホスト/ポート**: データベースサーバーの場所
   - **データベース名**: 対象データベース
   - **ユーザー名/パスワード**: データベース認証情報
4. 「接続テスト」をクリックして接続を確認
5. 今後の使用のために接続を保存

### 3. SQLクエリ実行
1. ダッシュボードから「SQL実行に進む」をクリック
2. データベース接続を選択
3. SQLクエリを入力：
   ```sql
   SELECT * FROM users WHERE age > :minAge AND status = :status
   ```
4. SqlApp2が自動的にパラメータ（`:minAge`、`:status`）を検出
5. 適切なデータ型でパラメータ値を入力
6. 「クエリ実行」をクリックしてSQLを実行
7. インタラクティブなテーブル形式で結果を表示

### 4. スキーマ情報閲覧
1. ダッシュボードから「スキーマ表示」をクリック
2. データベース接続を選択
3. データベース内のテーブル一覧を確認
4. 各テーブルをクリックしてカラム詳細を表示：
   - カラム名とデータ型
   - NULL許可・制約情報
   - カラムサイズ・精度情報
   - Primary Key・Foreign Key情報

### 5. 高度な機能
- **パラメータタイプ**: string、int、long、double、boolean、date、time、datetimeをサポート
- **クエリ検証**: リアルタイムSQL構文チェック
- **接続テスト**: クエリ実行前のデータベース接続確認
- **スキーマ参照**: SQLクエリ作成時のテーブル・カラム構造確認
- **レスポンシブデザイン**: デスクトップとモバイルデバイスでシームレスに動作

## 🔧 設定

### 環境変数

```bash
# データベース設定
DB_HOST=localhost
DB_PORT=3306
DB_NAME=myapp
DB_USER=username
DB_PASSWORD=password

# JWT設定
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400

# サーバー設定
SERVER_PORT=8080
```

### アプリケーションプロパティ

```properties
# H2データベース（内部）
spring.datasource.url=jdbc:h2:mem:sqlapp2db
spring.h2.console.enabled=true

# JPA設定
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# セキュリティ
app.jwt.secret=${JWT_SECRET:defaultSecretKey}
app.jwt.expiration=${JWT_EXPIRATION:86400}
```

## 🧪 テスト

### バックエンドテスト実行
```bash
./gradlew test
```

### フロントエンドテスト実行
```bash
cd frontend
npm test
```

### 統合テスト
```bash
# アプリケーション開始
./gradlew bootRun

# 統合テスト実行
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

## 📁 プロジェクト構造

```
sqlapp2/
├── src/main/java/cherry/sqlapp2/           # Spring Bootバックエンド
│   ├── controller/                         # REST APIコントローラー
│   ├── service/                           # ビジネスロジックサービス
│   ├── entity/                            # JPAエンティティ
│   ├── repository/                        # データアクセス層
│   ├── dto/                              # データ転送オブジェクト
│   ├── config/                           # 設定クラス
│   └── security/                         # セキュリティコンポーネント
├── src/main/resources/
│   ├── application.properties            # バックエンド設定
│   └── static/                          # ビルド済みフロントエンドアセット（自動生成）
├── frontend/                            # Reactアプリケーション
│   ├── src/
│   │   ├── components/                  # Reactコンポーネント
│   │   ├── context/                     # Reactコンテキストプロバイダー
│   │   └── utils/                       # ユーティリティ関数
│   ├── package.json                     # フロントエンド依存関係
│   └── vite.config.ts                   # Vite設定
├── build.gradle                         # Gradleビルド設定
├── Dockerfile                           # Dockerイメージ定義
├── docker-compose.yml                   # Docker Compose設定
└── README.md                           # このファイル
```

## 🤝 貢献

貢献を歓迎します！詳細については[貢献ガイド](CONTRIBUTING.md)をご覧ください。

### 開発セットアップ

1. リポジトリをフォーク
2. 機能ブランチを作成: `git checkout -b feature/amazing-feature`
3. コーディング標準に従って変更を行う
4. 新機能にテストを追加
5. 変更をコミット: `git commit -m 'すばらしい機能を追加'`
6. ブランチにプッシュ: `git push origin feature/amazing-feature`
7. プルリクエストを開く

### コードスタイル

- **Java**: Spring Bootの規約とベストプラクティスに従う
- **TypeScript/React**: フックを使用した関数型コンポーネントを使用
- **ドキュメント**: パブリックAPIにJSDoc/JavaDocを含める
- **テスト**: 新機能の高いテストカバレッジを維持

## 🐛 トラブルシューティング

### よくある問題

**接続拒否エラー:**
- データベースサーバーが実行されていることを確認
- 接続パラメータ（ホスト、ポート、認証情報）を検証
- ファイアウォール設定を確認

**JWTトークン期限切れ:**
- 認証トークンを更新するため再ログイン
- システムクロックが同期されているか確認

**ビルド失敗:**
- Java 21とNode.js 18+がインストールされていることを確認
- Gradleキャッシュをクリア: `./gradlew clean`
- npmキャッシュをクリア: `npm cache clean --force`

**H2コンソールアクセス（開発時）:**
- http://localhost:8080/h2-console にアクセス
- JDBC URL: `jdbc:h2:mem:sqlapp2db`
- ユーザー名: `sa`、パスワード: （空）

### サポート

- 📚 [ドキュメント](docs/)
- 🐛 [課題トラッカー](https://github.com/your-username/sqlapp2/issues)
- 💬 [ディスカッション](https://github.com/your-username/sqlapp2/discussions)

## 📜 ライセンス

このプロジェクトはApache License 2.0の下でライセンスされています - 詳細は[LICENSE](LICENSE)ファイルをご覧ください。

## 🙏 謝辞

- [Spring Boot](https://spring.io/projects/spring-boot) - エンタープライズJavaフレームワーク
- [React](https://reactjs.org/) - UIコンポーネントライブラリ
- [Vite](https://vitejs.dev/) - 高速ビルドツール
- [H2 Database](https://www.h2database.com/) - 組み込みデータベース
- [JWT.io](https://jwt.io/) - JSON Web Tokens

## 🎯 ロードマップ

### ✅ フェーズ1-2: MVP（完了）
- [x] JWTによるユーザー認証
- [x] マルチデータベース接続管理
- [x] パラメータ化クエリを使用したSQL実行
- [x] スキーマ情報取得とブラウジング
- [x] モダンReactフロントエンド
- [x] Dockerデプロイサポート

### ✅ フェーズ3.1: クエリ管理（完了）
- [x] パラメータテンプレート付きSQLクエリ保存・管理
- [x] アクセス制御によるパブリック/プライベートクエリ共有
- [x] パフォーマンスメトリクス付き自動実行履歴追跡
- [x] クエリ統計ダッシュボードと分析
- [x] パラメータ復元によるワンクリッククエリ再実行
- [x] レスポンシブデザインによる完全UI/UX実装

### 🔄 フェーズ3.2+: 高度機能（オプション）
- [ ] クエリ履歴とお気に入り
- [ ] SQLクエリビルダーインターフェース
- [ ] 高度な結果エクスポート（CSV、JSON、Excel）
- [ ] クエリパフォーマンス分析

### 🚀 フェーズ4: エンタープライズ機能（オプション）
- [ ] ユーザーロール管理
- [ ] 監査ログとコンプライアンス
- [ ] パフォーマンス分析ダッシュボード
- [ ] クエリ最適化提案

---

**❤️ SqlApp2チームによって作成**

*SqlApp2 - モダンでセキュアなSQLツールでデータベース専門家をエンパワー*