# Request Handling Application

Camel for Spring Boot + Undertow + Servletコンシューマエンドポイントを使用した、カスタムエラーハンドリング機能を持つAPIアプリケーションです。

## ドキュメント

- **[カスタムエラーハンドリングガイド](CUSTOM_ERROR_GUIDE.md)** - 404/405エラーのカスタマイズ方法
- **[API使用例](API_EXAMPLES.md)** - curlコマンドを使ったAPI呼び出し例
- **[実装ガイド](IMPLEMENTATION_GUIDE.md)** - アーキテクチャと実装の詳細

## 機能

- **Undertow Webサーバ**: 軽量で高性能なWebサーバ
- **Apache Camel Servletコンシューマ**: REST APIエンドポイントの公開
- **XML IO DSL**: Camelルートの宣言的な定義
- **カスタムエラーハンドリング**: 404, 405などのHTTPエラーに対する独自のエラーメッセージ
- **JSON形式のエラーレスポンス**: 構造化されたエラーレスポンス
- **インメモリユーザー管理**: 完全なCRUD操作

## 技術スタック

- **Java**: OpenJDK 17.0.14
- **Spring Boot**: 3.3.8
- **Apache Camel**: 4.8.3
- **Undertow**: 組み込みWebサーバ
- **Lombok**: ボイラープレートコード削減
- **Maven**: ビルドツール

## プロジェクト構成

```
request-handling/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/requesthandling/
│   │   │   ├── RequestHandlingApplication.java      # メインアプリケーション
│   │   │   ├── config/
│   │   │   │   └── ErrorMessageProperties.java     # エラーメッセージ設定
│   │   │   ├── controller/
│   │   │   │   └── CustomErrorController.java      # エラーハンドラー
│   │   │   ├── model/
│   │   │   │   ├── User.java                        # ユーザーDTO
│   │   │   │   ├── ApiResponse.java                # APIレスポンスDTO
│   │   │   │   └── ErrorResponse.java              # エラーレスポンスDTO
│   │   │   ├── processor/
│   │   │   │   ├── GlobalErrorProcessor.java       # グローバルエラー処理
│   │   │   │   ├── GetUsersProcessor.java          # ユーザー一覧取得
│   │   │   │   ├── GetUserByIdProcessor.java       # ユーザー詳細取得
│   │   │   │   ├── CreateUserProcessor.java        # ユーザー作成
│   │   │   │   ├── UpdateUserProcessor.java        # ユーザー更新
│   │   │   │   ├── DeleteUserProcessor.java        # ユーザー削除
│   │   │   │   └── HealthCheckProcessor.java       # ヘルスチェック
│   │   │   ├── route/
│   │   │   │   └── RestApiConfiguration.java       # REST API設定とルート定義
│   │   │   └── service/
│   │   │       └── UserService.java                # ユーザー管理サービス
│   │   └── resources/
│   │       └── application.yml                      # アプリケーション設定
│   └── test/
└── README.md
```

## アーキテクチャ

### Camel Java DSL + Processorパターン

**設計方針**:
- **REST設定とルート定義**: Java DSL（`RestApiConfiguration.java`）
- **ビジネスロジック**: Processorクラスに完全分離
- **設定**: application.yml

**注意**: Camel XML IO DSLには`<restConfiguration>`、`<rest>`、`<routeConfiguration>`がサポートされていないという制限があるため、Java DSLを使用しています。

### Processorパターン

各エンドポイントの処理ロジックはProcessorクラスとして実装され、ルート定義から参照されます：

```java
@Component
public class GetUsersProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        // ビジネスロジック
    }
}
```

**メリット**:
- ロジックとルーティングの明確な分離
- テスト可能な単位への分割
- 再利用性の向上
- 保守性の向上

## セットアップと起動

### 前提条件

- **JDK 17.0.14**以上
- **Maven 3.6**以上

### ビルドと実行

#### 方法1: Mavenで直接実行

```bash
# アプリケーションを起動
mvn spring-boot:run
```

#### 方法2: JARファイルから実行

```bash
# プロジェクトをビルド
mvn clean package -DskipTests

# JARファイルを実行
java -jar target/request-handling-1.0.0-SNAPSHOT.jar
```

アプリケーションは`http://localhost:8080`で起動します。

## APIエンドポイント

### ユーザー管理API

| メソッド | パス | 説明 |
|---------|------|------|
| GET | /api/users | ユーザー一覧を取得 |
| GET | /api/users/{id} | ユーザー詳細を取得 |
| POST | /api/users | 新規ユーザーを作成 |
| PUT | /api/users/{id} | ユーザー情報を更新 |
| DELETE | /api/users/{id} | ユーザーを削除 |

### ヘルスチェックAPI

| メソッド | パス | 説明 |
|---------|------|------|
| GET | /api/health | アプリケーションの状態を確認 |

### テスト用API

| メソッド | パス | 説明 |
|---------|------|------|
| GET | /api/test/error | 500エラーをテストするためのエンドポイント |

## テスト方法

### 正常なリクエスト

```bash
# ユーザー一覧取得
curl -X GET http://localhost:8080/api/users | jq

# ユーザー詳細取得
curl -X GET http://localhost:8080/api/users/1 | jq

# ユーザー作成
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"田中太郎","email":"tanaka@example.com"}' | jq

# ユーザー更新
curl -X PUT http://localhost:8080/api/users/4 \
  -H "Content-Type: application/json" \
  -d '{"name":"田中花子","email":"hanako@example.com"}' | jq

# ユーザー削除
curl -X DELETE http://localhost:8080/api/users/4 | jq

# ヘルスチェック
curl -X GET http://localhost:8080/api/health | jq
```

### エラーケースのテスト

```bash
# 404エラー（存在しないパス）
curl -X GET http://localhost:8080/api/nonexistent | jq

# 405エラー（許可されていないメソッド）
curl -X PATCH http://localhost:8080/api/users/1 | jq

# 404エラー（存在しないユーザー）
curl -X GET http://localhost:8080/api/users/999 | jq
```

### 期待されるレスポンス

**正常レスポンス例**:
```json
{
  "status": "success",
  "message": "ユーザー一覧を取得しました",
  "data": [
    {
      "id": "1",
      "name": "user1",
      "email": "user1@example.com"
    }
  ]
}
```

**404エラーレスポンス例**:
```json
{
  "code": 404,
  "message": "指定されたリソースが見つかりません",
  "details": "パス '/api/nonexistent' は存在しません。URLを確認してください。",
  "timestamp": "2025-11-10T12:00:00.123456",
  "path": "/api/nonexistent",
  "method": "GET"
}
```

**405エラーレスポンス例**:
```json
{
  "code": 405,
  "message": "許可されていないHTTPメソッドです",
  "details": "パス '/api/users/1' に対して、メソッド 'PATCH' は許可されていません。",
  "timestamp": "2025-11-10T12:00:00.123456",
  "path": "/api/users/1",
  "method": "PATCH"
}
```

## カスタマイズ方法

### 1. エラーメッセージの変更

`src/main/resources/application.yml`を編集：

```yaml
error:
  messages:
    404: "お探しのページは見つかりませんでした"
    405: "このメソッドは使用できません"
    500: "システムエラーが発生しました"
```

### 2. 新しいエンドポイントの追加

**Step 1**: Processorクラスを作成

```java
@Component
public class MyCustomProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        // 処理ロジック
    }
}
```

**Step 2**: `RestApiConfiguration.java`にルートを追加

```java
@Autowired
private MyCustomProcessor myCustomProcessor;

// configure()メソッド内
rest("/custom")
    .get()
        .to("direct:myCustomRoute");

from("direct:myCustomRoute")
    .log("カスタムリクエスト")
    .doTry()
        .process(myCustomProcessor)
    .doCatch(Exception.class)
        .setHeader("CamelHttpResponseCode", constant(500))
        .process(globalErrorProcessor)
    .end();
```

## エラーハンドリングの階層

### レベル1: Servletレベル（404, 405など）

**ハンドラー**: `CustomErrorController`

Spring BootのErrorControllerインターフェースを実装し、Servletレベルのエラーをキャッチします。

### レベル2: Camelルートレベル（ビジネスロジック内の例外）

**ハンドラー**: XMLの`<onException>`と`GlobalErrorProcessor`

Camelルート実行中に発生した例外をキャッチし、カスタムエラーレスポンスを返します。

## トラブルシューティング

### 問題1: Processorが見つからない

**エラー**: `No bean could be found in the registry for: xxxProcessor`

**解決方法**:
- Processorクラスに`@Component`アノテーションが付いているか確認
- Spring component scanの対象パッケージに含まれているか確認
- `RestApiConfiguration`で`@Autowired`されているか確認

### 問題2: バージョン互換性の問題

**確認事項**:
- JDK 17.0.14以上が使用されているか
- `pom.xml`のバージョンが正しいか
  - Spring Boot: 3.3.8
  - Camel: 4.8.3

### 問題3: XML IO DSL について

**制限事項**:
Camel XML IO DSLには以下の制限があります：
- `<restConfiguration>` サポートなし
- `<rest>` 定義サポートなし
- `<routeConfiguration>` サポートなし

そのため、本プロジェクトではJava DSLを使用しています。XML IO DSLは主にシンプルなルート定義用です。

## バージョン情報

- **Java**: OpenJDK 17.0.14
- **Spring Boot**: 3.3.8
- **Apache Camel**: 4.8.3
- **Undertow**: Spring Bootのデフォルトバージョン

## まとめ

この実装により、以下が実現できます：

✅ **Spring Boot 3.3.8 + Camel 4.8.3**: 最新バージョンの使用  
✅ **Processorパターン**: ビジネスロジックの完全分離  
✅ **カスタムエラーハンドリング**: 404、405などのHTTPエラーに対する独自レスポンス  
✅ **JSON形式の統一**: 構造化されたエラーとレスポンス形式  
✅ **設定ベース**: エラーメッセージをYAMLで管理  
✅ **完全なCRUD操作**: RESTful APIの実装  
✅ **Undertow**: 高性能な組み込みWebサーバ  
✅ **保守性**: ロジックとルーティングの明確な分離  

**技術的選択について**:
当初XML IO DSLの使用を検討しましたが、`<restConfiguration>`と`<rest>`定義がサポートされていない制限があるため、Java DSLを採用しました。この構成は、マイクロサービスアーキテクチャやAPI Gatewayパターンで使用されるベストプラクティスに従っています。
