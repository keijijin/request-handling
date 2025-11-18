# Servlet + REST DSL: 各エンドポイント独立構成（Java + XML IO DSL ハイブリッド）

## ✅ 結論

**Servletコンシューマエンドポイントでも、REST DSLを使用すれば、Platform HTTPと同じように各エンドポイントを独立したルートとして定義できます。**

404/405エラーのハンドリングも正常に動作します。

## 重要な認識の修正

### ❌ 誤った認識（No.49の回答）

「Servletではすべてのエンドポイントを1つのルートに統合する必要がある」

**これは誤りでした。**

### ✅ 正しい認識

**Servletでも、REST DSLを使用すれば、各エンドポイントを独立したルートとして定義できる。**

## 現在の実装方式：Java + XML IO DSL ハイブリッド

### XML IO DSLの制限事項

XML IO DSL（Camel 4.8）では、以下の要素はサポートされていません：

- ❌ `<restConfiguration>` 要素
- ✅ `<rests>` / `<rest>` 要素（ドキュメント上は可能だが、実装上は制約あり）

そのため、現在の実装は**Java DSLとXML IO DSLのハイブリッド構成**となっています。

### 実装構成

#### 1. REST設定とエンドポイント定義（Java DSL）

`config/RestApiConfiguration.java`

```java
@Configuration
public class RestApiConfiguration {

    @Bean
    public RouteBuilder restConfigurationRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // REST設定
                restConfiguration()
                    .component("servlet")
                    .bindingMode(RestBindingMode.off)
                    .contextPath("/api")
                    .enableCORS(true)
                    .dataFormatProperty("prettyPrint", "true");

                // RESTエンドポイント定義
                rest("/users")
                    .get("/").to("direct:get-users")
                    .post("/").to("direct:create-user")
                    .get("/{id}").to("direct:get-user-by-id")
                    .put("/{id}").to("direct:update-user")
                    .delete("/{id}").to("direct:delete-user");

                rest("/health")
                    .get("/").to("direct:health");

                rest("/test")
                    .get("/error").to("direct:test-error");
            }
        };
    }
}
```

#### 2. ルート実装（XML IO DSL）

`camel/routes.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<routes xmlns="http://camel.apache.org/schema/xml-io">

  <!-- ルート実装（direct経由でREST DSLから呼び出される） -->
  <route id="get-users-route">
    <from uri="direct:get-users"/>
    <log message="ユーザー一覧取得"/>
    <doTry>
      <process ref="getUsersProcessor"/>
      <doCatch>
        <exception>java.lang.Exception</exception>
        <setHeader name="CamelHttpResponseCode">
          <constant>500</constant>
        </setHeader>
        <setHeader name="Content-Type">
          <constant>application/json</constant>
        </setHeader>
        <process ref="globalErrorProcessor"/>
      </doCatch>
    </doTry>
  </route>

  <route id="create-user-route">
    <from uri="direct:create-user"/>
    <log message="ユーザー作成"/>
    <doTry>
      <process ref="createUserProcessor"/>
      <doCatch>
        <exception>java.lang.Exception</exception>
        <setHeader name="CamelHttpResponseCode">
          <constant>500</constant>
        </setHeader>
        <setHeader name="Content-Type">
          <constant>application/json</constant>
        </setHeader>
        <process ref="globalErrorProcessor"/>
      </doCatch>
    </doTry>
  </route>

  <!-- 他のルートも同様に実装... -->
</routes>
```

**特徴**:
- ✅ 各エンドポイントが独立したルート（7ルート）
- ✅ HTTPメソッドがJava DSLで明確（`.get()`, `.post()`, `.put()`, `.delete()`）
- ✅ ルート実装はXML IO DSLで記述可能
- ✅ 既存のREST DSL構成と同じ構造
- ✅ Platform HTTPの構成に近い
- ✅ 404/405エラーが自動的に処理される

### 方法2: 単一ルート + Choice（非推奨）❌

すべてのエンドポイントを1つのルートに統合

```xml
<route id="api-route">
  <from uri="servlet:/*?servletName=CamelServlet"/>
  <choice>
    <when>
      <simple>${header.CamelHttpPath} == '/users'</simple>
      <choice>
        <when>
          <simple>${header.CamelHttpMethod} == 'GET'</simple>
          <!-- ... -->
        </when>
        <when>
          <simple>${header.CamelHttpMethod} == 'POST'</simple>
          <!-- ... -->
        </when>
      </choice>
    </when>
    <!-- すべてのエンドポイントがここに... -->
  </choice>
</route>
```

**問題点**:
- ❌ 既存のREST DSL構成を破壊
- ❌ ネストした`<choice>`で複雑化
- ❌ 保守が困難
- ❌ Platform HTTPの構成とは大きく異なる

## 動作確認結果

### テスト実行

```bash
# 1. Health Check
curl http://localhost:8080/api/health
# → ✅ 200 OK

# 2. Users List
curl http://localhost:8080/api/users
# → ✅ 200 OK (4 users)

# 3. User by ID
curl http://localhost:8080/api/users/1
# → ✅ 200 OK

# 4. 404 Test
curl http://localhost:8080/api/nonexistent
# → ✅ 404 Not Found

# 5. 405 Test
curl -X PUT http://localhost:8080/api/health
# → ✅ 405 Method Not Allowed
```

**すべて正常に動作！**

## 404/405エラーハンドリングの仕組み

### REST DSLによる自動エラー処理

REST DSLを使用すると、Camelが以下のエラーを**自動的に処理**します：

#### 404 Not Found

定義されていないパスにアクセスした場合、Camelが自動的に404を返します。

```
GET /api/nonexistent
→ 404 {"code":404,"message":"指定されたリソースが見つかりません"}
```

**仕組み**:
1. Camel REST DSLがすべての定義済みパスを登録
2. 未定義のパスへのリクエストは、Camelがキャッチ
3. CustomErrorController（Spring Boot）が404レスポンスを生成

#### 405 Method Not Allowed

定義されていないHTTPメソッドでアクセスした場合、Camelが自動的に405を返します。

```
PUT /api/health  (定義されているのはGETのみ)
→ 405 {"code":405,"message":"許可されていないHTTPメソッドです"}
```

**仕組み**:
1. REST DSLで定義されたHTTPメソッド以外のリクエストをCamelがキャッチ
2. CustomErrorController（Spring Boot）が405レスポンスを生成

### Spring Boot CustomErrorController

```java
@RestController
public class CustomErrorController implements ErrorController {
    
    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String method = request.getMethod();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(statusCode != null ? statusCode : 500)
                .message(getErrorMessage(statusCode))
                .timestamp(LocalDateTime.now().toString())
                .path(path)
                .method(method)
                .build();
        
        return ResponseEntity.status(statusCode != null ? statusCode : 500)
                .body(errorResponse);
    }
}
```

**動作**:
1. Camelがエラーを検知（404/405）
2. Spring BootのエラーハンドラにフォワードされCustomErrorControllerが処理
3. JSON形式のエラーレスポンスを返却

#### 3. Servlet登録（Java）

`RequestHandlingApplication.java`

```java
@SpringBootApplication(exclude = ServletMappingAutoConfiguration.class)
public class RequestHandlingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RequestHandlingApplication.class, args);
    }
    
    /**
     * CamelServletの明示的な登録
     * /api/* のパスで受け付ける
     */
    @Bean
    public ServletRegistrationBean<CamelHttpTransportServlet> servletRegistrationBean() {
        ServletRegistrationBean<CamelHttpTransportServlet> registration = 
            new ServletRegistrationBean<>(new CamelHttpTransportServlet(), "/api/*");
        registration.setName("CamelServlet");
        return registration;
    }
}
```

**重要ポイント**:
- `ServletMappingAutoConfiguration`を除外して、Servletの二重登録を防止
- `/api/*`パスで明示的にServletを登録

#### 4. Spring Boot設定

`application.yml`

```yaml
server:
  port: 8080
  undertow:
    threads:
      io: 4
      worker: 20
  error:
    whitelabel:
      enabled: false

spring:
  application:
    name: request-handling-app
  mvc:
    throw-exception-if-no-handler-found: true
  web:
    resources:
      add-mappings: false

camel:
  springboot:
    name: RequestHandlingCamelContext
    xml-routes: "classpath:camel/*.xml"

error:
  messages:
    404: "指定されたリソースが見つかりません"
    405: "許可されていないHTTPメソッドです"
    500: "内部サーバーエラーが発生しました"
    default: "エラーが発生しました"
```

## REST DSL vs 単一ルートの比較

| 項目 | REST DSL（Java + XML IO DSL） | 単一ルート + Choice |
|-----|-------------------------------|-------------------|
| ルート数 | 7ルート（独立） | 1ルート（統合） |
| 実装ファイル数 | 2ファイル（Java + XML） | 1ファイル（XML） |
| 可読性 | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| 保守性 | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| 既存構成との互換性 | ✅ 維持可能 | ❌ 破壊的 |
| HTTPメソッド定義 | Java DSLメソッド（`.get()`, `.post()`等） | `<choice>`で条件分岐 |
| パスパラメータ | 自動抽出（`{id}`） | 手動抽出（`substring`） |
| 404/405処理 | 自動 | 手動実装 |
| 新規EP追加 | 簡単（Java DSL 1行 + XML 1ルート） | やや困難（XMLの`<choice>`修正） |
| Platform HTTP構成との類似性 | 高い | 低い |
| XML IO DSL完結度 | ⚠️ Java DSL必須 | ✅ XML IO DSLのみ |

## Platform HTTP構成との比較

### Platform HTTP（XML IO DSL）

```xml
<route id="get-users-route">
  <from uri="platform-http:/api/users?httpMethodRestrict=GET"/>
  <log message="ユーザー一覧取得リクエスト"/>
  <process ref="getUsersProcessor"/>
</route>

<route id="create-user-route">
  <from uri="platform-http:/api/users?httpMethodRestrict=POST"/>
  <log message="ユーザー作成: ${body}"/>
  <process ref="createUserProcessor"/>
</route>
```

**特徴**:
- ✅ 完全にXML IO DSLで記述可能
- ✅ `platform-http:` URIで直接エンドポイントを定義
- ✅ `httpMethodRestrict`でHTTPメソッドを指定

### Servlet + REST DSL（Java + XML IO DSL ハイブリッド）

**Java DSL（REST設定とエンドポイント定義）**:

```java
restConfiguration()
    .component("servlet")
    .bindingMode(RestBindingMode.off)
    .contextPath("/api");

rest("/users")
    .get("/").to("direct:get-users")
    .post("/").to("direct:create-user");
```

**XML IO DSL（ルート実装）**:

```xml
<route id="get-users-route">
  <from uri="direct:get-users"/>
  <log message="ユーザー一覧取得リクエスト"/>
  <process ref="getUsersProcessor"/>
</route>

<route id="create-user-route">
  <from uri="direct:create-user"/>
  <log message="ユーザー作成: ${body}"/>
  <process ref="createUserProcessor"/>
</route>
```

**特徴**:
- ⚠️ Java DSLとXML IO DSLのハイブリッド構成
- ✅ REST設定はJava DSLで記述（XML IO DSLの制限を回避）
- ✅ ルート実装はXML IO DSLで記述可能
- ✅ `.get()`, `.post()` メソッドでHTTPメソッドを指定

**類似点**:
- ✅ 各エンドポイントが独立したルート
- ✅ HTTPメソッドが明確
- ✅ 404/405が自動処理

**相違点**:
- Platform HTTP: 完全にXML IO DSL、`httpMethodRestrict`パラメータで指定
- Servlet + REST DSL: Java DSL（REST設定）+ XML IO DSL（ルート実装）、Java DSLメソッド（`.get()`, `.post()`）で指定

## 既存のREST DSL構成への影響

### ✅ 既存構成を維持可能

REST DSLを使用する限り、既存の構成を**そのまま維持**できます。

**例**: 既存のAPIがある場合

**Java DSL（`RestApiConfiguration.java`）**:

```java
@Bean
public RouteBuilder restConfigurationRouteBuilder() {
    return new RouteBuilder() {
        @Override
        public void configure() throws Exception {
            // REST設定（全体共通）
            restConfiguration()
                .component("servlet")
                .bindingMode(RestBindingMode.off)
                .contextPath("/api");

            // 既存のAPI
            rest("/orders")
                .get("/").to("direct:get-orders");

            // 新規追加のAPI
            rest("/users")
                .get("/").to("direct:get-users");
        }
    };
}
```

**XML IO DSL（`camel/routes.xml`）**:

```xml
<!-- 既存のルート -->
<route id="get-orders-route">
  <from uri="direct:get-orders"/>
  <!-- ... -->
</route>

<!-- 新規追加のルート -->
<route id="get-users-route">
  <from uri="direct:get-users"/>
  <!-- ... -->
</route>
```

**影響なし**: 各`rest()`ブロックと`<route>`は独立しており、相互に影響しません。

## エラーハンドリングの実装パターン

### 現在の実装: ルート内でエラー処理

**XML IO DSL（`camel/routes.xml`）**:

```xml
<route id="get-users-route">
  <from uri="direct:get-users"/>
  <log message="ユーザー一覧取得"/>
  <doTry>
    <process ref="getUsersProcessor"/>
    <doCatch>
      <exception>java.lang.Exception</exception>
      <setHeader name="CamelHttpResponseCode">
        <constant>500</constant>
      </setHeader>
      <setHeader name="Content-Type">
        <constant>application/json</constant>
      </setHeader>
      <process ref="globalErrorProcessor"/>
    </doCatch>
  </doTry>
</route>
```

**特徴**:
- ✅ 各ルートで個別にエラー処理
- ✅ きめ細かい制御が可能
- ✅ エラーレスポンスのカスタマイズが容易

### 代替案: グローバルエラーハンドラ

**XML IO DSL（`camel/routes.xml`）**:

```xml
<routes xmlns="http://camel.apache.org/schema/xml-io">

  <!-- グローバルエラーハンドラ -->
  <onException>
    <exception>java.lang.Exception</exception>
    <handled>
      <constant>true</constant>
    </handled>
    <setHeader name="CamelHttpResponseCode">
      <constant>500</constant>
    </setHeader>
    <setHeader name="Content-Type">
      <constant>application/json</constant>
    </setHeader>
    <process ref="globalErrorProcessor"/>
  </onException>

  <!-- ルート定義（エラーハンドリング不要） -->
  <route id="get-users-route">
    <from uri="direct:get-users"/>
    <log message="ユーザー一覧取得"/>
    <process ref="getUsersProcessor"/>
  </route>

</routes>
```

**特徴**:
- ✅ すべてのルートで統一的なエラー処理
- ✅ ルート定義がシンプル
- ⚠️ エラー処理のカスタマイズが困難

## まとめ

### 質問への回答

> 正常のリクエストを受信するservletコンシューマエンドポイントが既に複数ある前提で、それらの構成を壊さず404や405などをハンドリングすることは難しいのでしょうか。

**回答**: ✅ **可能です。REST DSLを使用すれば実現できます。**

### 現在の実装構成

**Java + XML IO DSL ハイブリッド構成**

1. **Java DSL**（`config/RestApiConfiguration.java`）
   - REST設定（`restConfiguration()`）
   - RESTエンドポイント定義（`rest().get()`, `rest().post()`, etc.）
   - `direct:` エンドポイントへの転送

2. **XML IO DSL**（`camel/routes.xml`）
   - `direct:` エンドポイントからの受信
   - ルート実装（ビジネスロジック）
   - エラーハンドリング（`doTry`/`doCatch`）

3. **Java**（`RequestHandlingApplication.java`）
   - Servlet明示的登録（`ServletRegistrationBean`）
   - Auto-configuration除外（`ServletMappingAutoConfiguration`）

### キーポイント

1. ✅ **REST DSLを使用する**（Java DSL）
   - `restConfiguration().component("servlet")`
   - `rest("/users").get("/").to("direct:get-users")`

2. ✅ **各エンドポイントを独立したルートとして定義**
   - 既存のREST DSL構成と同じ構造
   - Platform HTTPの構成に近い

3. ✅ **404/405エラーは自動的に処理**
   - REST DSLが未定義のパス/メソッドをキャッチ
   - CustomErrorControllerがJSONレスポンスを生成

4. ✅ **既存構成への影響なし**
   - 各`rest()`ブロックと`<route>`は独立
   - 新規追加が容易

### XML IO DSLの制限事項

XML IO DSL（Camel 4.8）では、`<restConfiguration>` 要素がサポートされていないため、REST設定とエンドポイント定義はJava DSLで記述する必要があります。

ただし、ルート実装（ビジネスロジック）はXML IO DSLで記述可能です。

### 推奨構成

**Servletを使用する場合は、必ずREST DSLを使用してください。**

単一ルート + Choice構成は、以下の理由で**非推奨**です：
- ❌ 既存のREST DSL構成を破壊
- ❌ 複雑で保守が困難
- ❌ 新規エンドポイント追加が困難

### アーキテクチャ図

```
[HTTP Request]
    ↓
[CamelServlet] (/api/*)
    ↓
[REST DSL] (Java) - RESTエンドポイント定義
    ↓
[direct:get-users] (転送)
    ↓
[Route] (XML IO DSL) - ルート実装
    ↓
[Processor] (Java) - ビジネスロジック
    ↓
[HTTP Response]
```

---

**作成日**: 2025-11-17  
**更新日**: 2025-11-17  
**プロジェクト**: Apache Camel 4 REST API実装  
**バージョン**: 3.0.0 (現在の実装ベース)

