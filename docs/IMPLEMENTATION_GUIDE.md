# 実装ガイド - リクエストハンドリング用アプリケーション

## 概要

このドキュメントでは、Camel for Spring Boot + Undertow + Platform HTTPを使用して、404や405などのエラーレスポンスをカスタマイズする方法を説明します。

## アーキテクチャ

### 技術スタック

- **Webサーバ**: Undertow（軽量で高性能）
- **フレームワーク**: Spring Boot 3.3.8
- **統合フレームワーク**: Apache Camel 4.8.3
- **エンドポイント**: Camel Platform HTTP
- **Java**: OpenJDK 17.0.14

### コンポーネント構成

```
┌─────────────────────────────────────────────────────┐
│                  クライアント                          │
└───────────────────┬─────────────────────────────────┘
                    │ HTTP Request
                    ▼
┌─────────────────────────────────────────────────────┐
│              Undertow Webサーバ                      │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│         Camel Platform HTTP (/api/*)                │
│                                                       │
│  ┌───────────────────────────────────────────┐     │
│  │        正常なルート処理                     │     │
│  │   (routes.xml - XML IO DSL)                │     │
│  └───────────────┬───────────────────────────┘     │
│                  │ 404/405エラー発生                │
│                  ▼                                   │
│  ┌───────────────────────────────────────────┐     │
│  │    CustomErrorController                   │     │
│  │  (Spring Boot ErrorController)             │     │
│  └───────────────┬───────────────────────────┘     │
└──────────────────┼─────────────────────────────────┘
                   │
                   ▼
          カスタムエラーレスポンス
         (JSON形式、詳細情報付き)
```

## エラーハンドリングの実装

### 1. CustomErrorController

Spring BootのErrorControllerインターフェースを実装し、Servletレベルのエラーをキャッチします。

**ファイル**: `src/main/java/com/example/requesthandling/controller/CustomErrorController.java`

```java
@RestController
public class CustomErrorController implements ErrorController {

    private final ErrorMessageProperties errorMessageProperties;

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        // HTTPステータスコード取得
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;
        
        // カスタムエラーメッセージを取得
        String customMessage = errorMessageProperties.getMessage(statusCode);
        
        // エラーレスポンスを構築
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(statusCode)
                .message(customMessage)
                .details(getDetailMessage(statusCode, path, method))
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .build();
        
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
}
```

**処理フロー**:
1. エラー発生時、Springが自動的に`/error`エンドポイントに転送
2. HTTPステータスコードとリクエスト情報を取得
3. `application.yml`から対応するエラーメッセージを取得
4. JSON形式のエラーレスポンスを構築して返却

### 2. ErrorMessageProperties

`application.yml`からエラーメッセージを読み込む設定クラスです。

**ファイル**: `src/main/java/com/example/requesthandling/config/ErrorMessageProperties.java`

```java
@Component
@ConfigurationProperties(prefix = "error")
@Data
public class ErrorMessageProperties {
    
    private Map<String, String> messages = new HashMap<>();
    
    public String getMessage(int statusCode) {
        return messages.getOrDefault(
            String.valueOf(statusCode), 
            messages.getOrDefault("default", "エラーが発生しました")
        );
    }
}
```

**設定例** (`application.yml`):

```yaml
error:
  messages:
    404: "指定されたリソースが見つかりません"
    405: "許可されていないHTTPメソッドです"
    500: "内部サーバーエラーが発生しました"
    default: "エラーが発生しました"
```

### 3. ErrorResponse DTO

統一されたエラーレスポンス形式を定義します。

**ファイル**: `src/main/java/com/example/requesthandling/model/ErrorResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int code;              // HTTPステータスコード
    private String message;         // エラーメッセージ
    private String details;         // 詳細情報
    private String timestamp;       // エラー発生日時（ISO 8601形式の文字列）
    private String path;            // リクエストパス
    private String method;          // HTTPメソッド
}
```

### 4. Camelルート定義（XML IO DSL + Processorパターン）

APIエンドポイントを定義し、ビジネスロジックはProcessorクラスに委譲します。このプロジェクトは**XML IO DSL**を使用しています。

**重要な実装ポイント**:
1. **Platform HTTP**: 各ルートで直接HTTPエンドポイントを定義
2. **HTTPメソッド制限**: `httpMethodRestrict`パラメータで制限
3. **404/405統一JSON**: `spring.mvc.throw-exception-if-no-handler-found=true`等の設定
4. **パスパラメータ**: `platform-http:/api/users/{id}` で定義可能

**ファイル**: `src/main/resources/camel/routes.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<routes xmlns="http://camel.apache.org/schema/xml-io">

  <!-- ユーザー一覧取得 -->
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

  <!-- ユーザー詳細取得 -->
  <route id="get-user-by-id-route">
    <from uri="platform-http:/api/users/{id}?httpMethodRestrict=GET"/>
    <log message="ユーザー詳細取得: ID=${header.id}"/>
    <doTry>
      <process ref="getUserByIdProcessor"/>
      <doCatch>
        <exception>java.lang.Exception</exception>
        <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
        <setHeader name="Content-Type"><constant>application/json</constant></setHeader>
        <process ref="globalErrorProcessor"/>
      </doCatch>
    </doTry>
  </route>

  <!-- 他のルート... -->

</routes>
```

**アプリケーションクラス設定**:

```java
@SpringBootApplication
public class RequestHandlingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RequestHandlingApplication.class, args);
    }
}
```

**注意**: XML IO DSLは`camel.springboot.xml-routes`設定で自動読み込みされます。

**application.yml設定**:

```yaml
server:
  error:
    whitelabel:
      enabled: false

spring:
  mvc:
    throw-exception-if-no-handler-found: true
  web:
    resources:
      add-mappings: false

camel:
  springboot:
    name: RequestHandlingCamelContext
    xml-routes: "classpath:camel/*.xml"
  component:
    platform-http:
      platform-http-engine: undertow
```

**Processorクラス例**:

```java
@Component
public class GetUsersProcessor implements Processor {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        ApiResponse response = ApiResponse.builder()
                .status("success")
                .message("ユーザー一覧を取得しました")
                .data(userService.getAllUsers())
                .build();
        
        String jsonResponse = objectMapper.writeValueAsString(response);
        exchange.getMessage().setBody(jsonResponse);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
```

## エラーハンドリングの階層

### レベル1: Servletレベル（404, 405など）

**対象エラー**:
- 404 Not Found - 存在しないパスへのアクセス
- 405 Method Not Allowed - 許可されていないHTTPメソッド
- その他のHTTPエラー

**ハンドラー**: CustomErrorController

**動作**:
1. Undertow/Servletコンテナがエラーを検知
2. Spring Bootが`/error`エンドポイントに転送
3. CustomErrorControllerがエラーを処理
4. カスタムJSON レスポンスを返却

### レベル2: Camelルートレベル（ビジネスロジック内の例外）

**対象エラー**:
- ビジネスロジック内で発生する例外
- データベース接続エラー
- 外部API呼び出しエラー

**ハンドラー**: `routes.xml`内の`<doCatch>`と`GlobalErrorProcessor`

**動作**:
1. Camelルート実行中に例外発生
2. `<doCatch>`がキャッチ
3. `GlobalErrorProcessor`で500エラーレスポンスを構築
4. クライアントに返却

## 実装のポイント

### 1. シンプルな設定

✅ **推奨** (application.ymlで設定):

```java
@SpringBootApplication
public class RequestHandlingApplication {
    // 追加設定は不要
}
```

```yaml
camel:
  springboot:
    name: RequestHandlingCamelContext
    xml-routes: "classpath:camel/*.xml"
  component:
    platform-http:
      platform-http-engine: undertow
```

### 2. API documentationを使う場合

API documentation機能を使用する場合は、`camel-openapi-java`依存関係が必要です。

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-openapi-java-starter</artifactId>
    <version>${camel.version}</version>
</dependency>
```

```java
restConfiguration()
        .component("servlet")
        .bindingMode(RestBindingMode.json)
        .contextPath("/api")
        .apiContextPath("/api-doc")  // API documentationを有効化
        .apiProperty("api.title", "Request Handling API")
        .apiProperty("api.version", "1.0.0");
```

### 3. エラーメッセージの国際化

将来的に多言語対応が必要な場合は、SpringのMessageSourceを使用します。

```java
@Component
public class CustomErrorController implements ErrorController {
    
    private final MessageSource messageSource;
    
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request, Locale locale) {
        String message = messageSource.getMessage(
            "error." + statusCode, 
            null, 
            locale
        );
        // ...
    }
}
```

## テスト方法

### 1. アプリケーションの起動

```bash
mvn spring-boot:run
```

### 2. 正常なリクエスト

```bash
curl -X GET http://localhost:8080/api/health
```

**期待されるレスポンス**:
```json
{
  "status": "UP",
  "message": "アプリケーションは正常に稼働しています",
  "data": "2025-11-10T10:30:00"
}
```

### 3. 404エラーのテスト

```bash
curl -X GET http://localhost:8080/api/nonexistent
```

**期待されるレスポンス**:
```json
{
  "code": 404,
  "message": "指定されたリソースが見つかりません",
  "details": "パス '/api/nonexistent' は存在しません。URLを確認してください。",
  "timestamp": "2025-11-10T10:30:00",
  "path": "/api/nonexistent",
  "method": "GET"
}
```

### 4. 405エラーのテスト

```bash
curl -X PATCH http://localhost:8080/api/users/123
```

**期待されるレスポンス**:
```json
{
  "code": 405,
  "message": "許可されていないHTTPメソッドです",
  "details": "パス '/api/users/123' に対して、メソッド 'PATCH' は許可されていません。",
  "timestamp": "2025-11-10T10:30:00",
  "path": "/api/users/123",
  "method": "PATCH"
}
```

## XMLルートの編集方法

### 直接編集

本プロジェクトの`src/main/resources/camel/routes.xml`は、XML IO DSL形式で記述されており、XMLエディタで直接編集します。

**手順**:

1. **XMLファイルを開く**
   ```
   src/main/resources/camel/routes.xml
   ```

2. **ルートを追加/編集**
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

3. **Processorクラスを実装**
   ```java
   @Component
   public class MyProcessor implements Processor {
       @Override
       public void process(Exchange exchange) throws Exception {
           // ビジネスロジック
       }
   }
   ```

**メリット**:
- シンプルで直感的な構造
- 各ルートが独立しており理解しやすい
- HTTPエンドポイントを直接定義
- エラーハンドリングが統一されている

**注意点**:
- Processorクラスのビジネスロジック自体は、Javaコードで実装する必要があります
- XMLで定義できるのはルーティングとフロー制御のみです

## カスタマイズ方法

### エラーメッセージの変更

`src/main/resources/application.yml`を編集:

```yaml
error:
  messages:
    404: "お探しのページは見つかりませんでした"
    405: "このメソッドは使用できません"
    500: "システムエラーが発生しました"
    401: "認証が必要です"  # 新しいエラーコードを追加
    403: "アクセスが拒否されました"
```

### エラーレスポンス形式の変更

`ErrorResponse.java`を修正して、必要なフィールドを追加:

```java
@Data
@Builder
public class ErrorResponse {
    private int code;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
    private String method;
    
    // 追加フィールド
    private String traceId;        // トレースID
    private String supportUrl;     // サポートページURL
    private List<String> suggestions; // 推奨アクション
}
```

### 新しいエンドポイントの追加

**Step 1**: Processorクラスを作成

```java
@Component
public class GetProductsProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        // 商品一覧取得ロジック
    }
}
```

**Step 2**: `routes.xml`に追加

```xml
<!-- 商品一覧取得 -->
<route id="get-products-route">
    <from uri="platform-http:/api/products?httpMethodRestrict=GET"/>
    <log message="商品一覧取得リクエスト"/>
    <doTry>
        <process ref="getProductsProcessor"/>
        <doCatch>
            <exception>java.lang.Exception</exception>
            <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
            <setHeader name="Content-Type"><constant>application/json</constant></setHeader>
            <process ref="globalErrorProcessor"/>
        </doCatch>
    </doTry>
</route>

<!-- 商品詳細取得 -->
<route id="get-product-by-id-route">
    <from uri="platform-http:/api/products/{id}?httpMethodRestrict=GET"/>
    <log message="商品詳細取得: ID=${header.id}"/>
    <doTry>
        <process ref="getProductByIdProcessor"/>
        <doCatch>
            <exception>java.lang.Exception</exception>
            <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
            <setHeader name="Content-Type"><constant>application/json</constant></setHeader>
            <process ref="globalErrorProcessor"/>
        </doCatch>
    </doTry>
</route>
```

**重要**: Platform HTTPでは、同一パスで複数のHTTPメソッドをサポートします。`httpMethodRestrict`パラメータで制御します。

## トラブルシューティング

### 問題1: Bean定義の重複エラー

**エラーメッセージ**:
```
Invalid bean definition with name 'camelServletRegistrationBean'
```

**解決方法**:
メインクラスから`@Bean`定義を削除し、`application.yml`で設定します。

### 問題2: 404エラーがカスタマイズされない

**確認ポイント**:
1. CustomErrorControllerが`@RestController`アノテーションを持っているか
2. `@RequestMapping("/error")`が正しく設定されているか
3. ErrorMessagePropertiesが`@Component`でSpringに登録されているか

### 問題3: Camelルートが起動しない

**確認ポイント**:
1. `camel-platform-http-starter`依存関係が追加されているか
2. `camel-xml-io-dsl`依存関係が追加されているか
3. `camel.springboot.xml-routes`設定が正しいか
4. XMLファイルのスキーマが正しいか（XML IO DSLの場合は`http://camel.apache.org/schema/xml-io`）
5. ログを確認してエラーメッセージを特定

### 問題4: XMLルートが読み込まれない

**エラーメッセージ**:
```
No routes found
```

**解決方法**:
1. `camel.springboot.xml-routes`設定が正しく設定されているか確認:
   ```yaml
   camel:
     springboot:
       xml-routes: "classpath:camel/*.xml"
   ```

2. XMLファイルが正しいディレクトリ（`src/main/resources/camel/`）に配置されているか確認

3. XMLのルート要素が`<routes>`で、XML IO DSL名前空間が正しく宣言されているか確認:
   ```xml
   <routes xmlns="http://camel.apache.org/schema/xml-io">
   ```

4. Platform HTTP設定が正しく設定されているか確認（`application.yml`）:
   ```yaml
   camel:
     component:
       platform-http:
         platform-http-engine: undertow
   ```

### 問題5: Processorが見つからない

**エラーメッセージ**:
```
No bean could be found in the registry of type: com.example.requesthandling.processor.GetUsersProcessor
```

**解決方法**:
1. Processorクラスに`@Component`アノテーションが付いているか確認
2. Processorクラスのパッケージが`@SpringBootApplication`のコンポーネントスキャン対象になっているか確認
3. XMLで参照している`ref`名がProcessorクラスのBean名（デフォルトはクラス名の先頭を小文字にしたもの）と一致しているか確認

### 問題6: パスパラメータの定義

**質問**: パスパラメータ（`{id}`）をどう定義すべきか？

**解決方法**:
Platform HTTPでは、URIに直接パスパラメータを含めます：

```xml
<route id="get-user-by-id-route">
    <from uri="platform-http:/api/users/{id}?httpMethodRestrict=GET"/>
    <log message="ユーザー詳細取得: ID=${header.id}"/>
    <doTry>
        <process ref="getUserByIdProcessor"/>
        <doCatch>
            <exception>java.lang.Exception</exception>
            <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
            <setHeader name="Content-Type"><constant>application/json</constant></setHeader>
            <process ref="globalErrorProcessor"/>
        </doCatch>
    </doTry>
</route>
```

パスパラメータは`${header.id}`でアクセスできます。

## まとめ

この実装により、以下が実現できます：

✅ 404、405などのHTTPエラーに対するカスタムレスポンス
✅ JSON形式の統一されたエラー形式
✅ 設定ファイルでエラーメッセージを管理
✅ Camelルート内のエラーハンドリング
✅ 詳細なエラー情報の提供
✅ **XML IO DSL**: シンプルで直感的なルート定義
✅ Processorパターンによるロジックとルーティングの分離
✅ **Platform HTTP**: Undertow上で動作する高性能HTTPコンポーネント
✅ **Undertow**: 高性能Webサーバの利用（Tomcatから切り替え）

この構成は、マイクロサービスアーキテクチャやAPI Gatewayパターンで使用されるエラーハンドリングのベストプラクティスに従っており、さらにXML IO DSLによるシンプルな設計によって保守性と理解しやすさを実現しています。

