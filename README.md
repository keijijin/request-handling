# Request Handling Application

Camel for Spring Boot + Undertow + Platform HTTPを使用した、カスタムエラーハンドリング機能を持つAPIアプリケーションです。

## ドキュメント

- **[カスタムエラーハンドリングガイド](CUSTOM_ERROR_GUIDE.md)** - 404/405エラーのカスタマイズ方法
- **[API使用例](API_EXAMPLES.md)** - curlコマンドを使ったAPI呼び出し例
- **[実装ガイド](IMPLEMENTATION_GUIDE.md)** - アーキテクチャと実装の詳細

## 機能

- **Undertow Webサーバ**: 軽量で高性能なWebサーバ（デフォルトのTomcatから切り替え）
- **Apache Camel Platform HTTP**: REST APIエンドポイントの公開
- **XML IO DSL**: Camelルートの宣言的な定義
- **カスタムエラーハンドリング**: 404, 405などのHTTPエラーに対する独自のエラーメッセージ
- **JSON形式のエラーレスポンス**: 構造化されたエラーレスポンス
- **インメモリユーザー管理**: 完全なCRUD操作

## 技術スタック

- **Java**: OpenJDK 17.0.14
- **Spring Boot**: 3.3.8
- **Apache Camel**: 4.8.3
  - `camel-spring-boot-starter`: Camel Spring Boot統合
  - `camel-platform-http-starter`: Platform HTTP統合
  - `camel-jackson-starter`: JSON処理
  - `camel-xml-io-dsl`: XML IO DSL
- **Undertow**: 組み込みWebサーバ（`pom.xml`でTomcatを除外し明示的に追加）
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
│   │   │   │   ├── HealthCheckProcessor.java       # ヘルスチェック
│   │   │   │   └── TestErrorProcessor.java         # テストエラー処理
│   │   │   └── service/
│   │   │       └── UserService.java                # ユーザー管理サービス
│   │   └── resources/
│   │       ├── application.yml                      # アプリケーション設定
│   │       └── camel/
│   │           └── routes.xml                       # Camelルート定義（XML IO DSL）
│   └── test/
└── README.md
```

## アーキテクチャ

### XML IO DSL + Processorパターン

**設計方針**:
- **ルート定義**: XML IO DSL（`routes.xml`）
- **ビジネスロジック**: Processorクラスに完全分離
- **設定**: application.yml

**重要な設定**:
- **Platform HTTP**: `camel-platform-http-starter`を使用してUndertow上で動作
- **HTTPメソッド制限**: `httpMethodRestrict`パラメータで各ルートのHTTPメソッドを制限
- **404/405統一JSON**: `spring.mvc.throw-exception-if-no-handler-found=true`等の設定が必要
- **XML IO DSL読み込み**: `camel.springboot.xml-routes`設定で自動読み込み

### Processorパターン

各エンドポイントの処理ロジックはProcessorクラスとして実装され、XMLルート定義から参照されます：

```java
@Component
public class GetUsersProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        // ビジネスロジック
    }
}
```

```xml
<!-- routes.xml -->
<route id="get-users-route">
    <from uri="platform-http:/api/users?httpMethodRestrict=GET"/>
    <log message="ユーザー一覧取得リクエスト"/>
    <doTry>
        <process ref="getUsersProcessor"/>
        <doCatch>
            <exception>java.lang.Exception</exception>
            <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
            <setHeader name="Content-Type"><constant>application/json</constant></setHeader>
            <process ref="globalErrorProcessor"/>
        </doCatch>
    </doTry>
</route>
```

**メリット**:
- ロジックとルーティングの明確な分離
- テスト可能な単位への分割
- 再利用性の向上
- 保守性の向上
- Platform HTTPによる高性能な処理

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

**Step 2**: `routes.xml`にルートを追加

```xml
<!-- カスタムルート -->
<route id="my-custom-route">
    <from uri="platform-http:/api/custom?httpMethodRestrict=GET"/>
    <log message="カスタムリクエスト"/>
    <doTry>
        <process ref="myCustomProcessor"/>
        <doCatch>
            <exception>java.lang.Exception</exception>
            <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
            <setHeader name="Content-Type"><constant>application/json</constant></setHeader>
            <process ref="globalErrorProcessor"/>
        </doCatch>
    </doTry>
</route>
```

## エラーハンドリングの階層

### レベル1: Servletレベル（404, 405など）

**ハンドラー**: `CustomErrorController`

Spring BootのErrorControllerインターフェースを実装し、Servletレベルのエラーをキャッチします。

### レベル2: Camelルートレベル（ビジネスロジック内の例外）

**ハンドラー**: XMLの`<doCatch>`と`GlobalErrorProcessor`

Camelルート実行中に発生した例外をキャッチし、カスタムエラーレスポンスを返します。

## トラブルシューティング

### 問題1: Processorが見つからない

**エラー**: `No bean could be found in the registry for: xxxProcessor`

**解決方法**:
- Processorクラスに`@Component`アノテーションが付いているか確認
- Spring component scanの対象パッケージに含まれているか確認
- アプリケーションが正常に起動しているか確認

### 問題2: バージョン互換性の問題

**確認事項**:
- JDK 17.0.14以上が使用されているか
- `pom.xml`のバージョンが正しいか
  - Spring Boot: 3.3.8
  - Camel: 4.8.3

### 問題3: XML IO DSL について

**重要なポイント**:
このプロジェクトは**XML IO DSL**を使用しています：

| 項目 | 説明 |
|------|------|
| ルート要素 | `<routes>` |
| HTTPコンポーネント | `platform-http` |
| エンドポイント定義 | `<from uri="platform-http:/api/path?httpMethodRestrict=GET"/>` |
| エラーハンドリング | `<doTry>`/`<doCatch>` |
| パスパラメータ | `platform-http:/api/users/{id}` |
| 読み込み方法 | `camel.springboot.xml-routes`設定 |
| 用途 | シンプルで直感的なルート定義 |

**XML IO DSLの注意点**:
- 各ルートで直接HTTPエンドポイントを定義
- `httpMethodRestrict`パラメータでHTTPメソッドを制限
- Platform HTTPコンポーネントにより、同一パスで複数のHTTPメソッドをサポート

## バージョン情報

- **Java**: OpenJDK 17.0.14
- **Spring Boot**: 3.3.8
- **Apache Camel**: 4.8.3
- **Undertow**: Spring Boot 3.3.8に含まれるバージョン（Tomcatを除外して使用）

## XMLルートの編集

このプロジェクトのCamelルート（`src/main/resources/camel/routes.xml`）は、XML IO DSL形式で記述されています。

### 編集方法

XMLファイルを直接編集します：

```
src/main/resources/camel/routes.xml
```

### ルート追加の例

```xml
<route id="my-new-route">
    <from uri="platform-http:/api/myendpoint?httpMethodRestrict=GET"/>
    <log message="新しいエンドポイント"/>
    <doTry>
        <process ref="myProcessor"/>
        <doCatch>
            <exception>java.lang.Exception</exception>
            <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
            <setHeader name="Content-Type"><constant>application/json</constant></setHeader>
            <process ref="globalErrorProcessor"/>
        </doCatch>
    </doTry>
</route>
```

### ポイント

- ✅ **シンプルな構造**: 各ルートが独立しており理解しやすい
- ✅ **直接的なエンドポイント定義**: `platform-http`で直接HTTPエンドポイントを指定
- ✅ **HTTPメソッド制限**: `httpMethodRestrict`パラメータで制御
- ✅ **統一されたエラーハンドリング**: 全ルートで`<doCatch>`を使用

## まとめ

この実装により、以下が実現できます：

✅ **Spring Boot 3.3.8 + Camel 4.8.3**: 最新バージョンの使用  
✅ **XML IO DSL**: シンプルで直感的なルート定義  
✅ **Platform HTTP**: Undertow上で動作する高性能HTTPコンポーネント  
✅ **Processorパターン**: ビジネスロジックの完全分離  
✅ **カスタムエラーハンドリング**: 404、405などのHTTPエラーに対する統一JSON形式  
✅ **JSON形式の統一**: 構造化されたエラーとレスポンス形式  
✅ **設定ベース**: エラーメッセージをYAMLで管理  
✅ **完全なCRUD操作**: RESTful APIの実装  
✅ **Undertow**: 高性能な組み込みWebサーバ（Tomcatから切り替え）  
✅ **保守性**: ロジックとルーティングの明確な分離  

**技術的選択について**:
このプロジェクトは**XML IO DSL**を採用しており、各ルートで直接HTTPエンドポイントを定義することで、シンプルで理解しやすい構造を実現しています。Platform HTTPコンポーネントにより、同一パスで複数のHTTPメソッドをサポートし、Processorパターンによりビジネスロジックとルーティングを分離し、`application.yml`での適切な設定（404/405統一JSON等）により、保守性の高い設計を実現しています。
