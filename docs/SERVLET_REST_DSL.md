# Servlet + REST DSL: 各エンドポイント独立構成

## ✅ 結論

**Servletコンシューマエンドポイントでも、REST DSLを使用すれば、Platform HTTPと同じように各エンドポイントを独立したルートとして定義できます。**

404/405エラーのハンドリングも正常に動作します。

## 重要な認識の修正

### ❌ 誤った認識（No.49の回答）

「Servletではすべてのエンドポイントを1つのルートに統合する必要がある」

**これは誤りでした。**

### ✅ 正しい認識

**Servletでも、REST DSLを使用すれば、各エンドポイントを独立したルートとして定義できる。**

## 実装方法の比較

### 方法1: REST DSL（推奨）✅

各エンドポイントが独立したルート、既存のREST DSL構成を維持可能

```xml
<?xml version="1.0" encoding="UTF-8"?>
<routes xmlns="http://camel.apache.org/schema/xml-io">

  <!-- REST設定：Servletを使用 -->
  <restConfiguration component="servlet" 
                     bindingMode="off" 
                     contextPath="/api"
                     enableCORS="true">
    <dataFormatProperty key="prettyPrint" value="true"/>
  </restConfiguration>

  <!-- RESTエンドポイント定義 -->
  <rests>
    <rest path="/users">
      <!-- GET /api/users -->
      <get uri="/">
        <route id="get-users-route">
          <log message="ユーザー一覧取得リクエスト"/>
          <process ref="getUsersProcessor"/>
        </route>
      </get>
      
      <!-- POST /api/users -->
      <post uri="/">
        <route id="create-user-route">
          <log message="ユーザー作成: ${body}"/>
          <process ref="createUserProcessor"/>
        </route>
      </post>
      
      <!-- GET /api/users/{id} -->
      <get uri="/{id}">
        <route id="get-user-by-id-route">
          <log message="ユーザー詳細取得: ID=${header.id}"/>
          <process ref="getUserByIdProcessor"/>
        </route>
      </get>
      
      <!-- PUT /api/users/{id} -->
      <put uri="/{id}">
        <route id="update-user-route">
          <log message="ユーザー更新: ID=${header.id}"/>
          <process ref="updateUserProcessor"/>
        </route>
      </put>
      
      <!-- DELETE /api/users/{id} -->
      <delete uri="/{id}">
        <route id="delete-user-route">
          <log message="ユーザー削除: ID=${header.id}"/>
          <process ref="deleteUserProcessor"/>
        </route>
      </delete>
    </rest>
    
    <rest path="/health">
      <get uri="/">
        <route id="health-route">
          <log message="ヘルスチェック"/>
          <process ref="healthCheckProcessor"/>
        </route>
      </get>
    </rest>
  </rests>
</routes>
```

**特徴**:
- ✅ 各エンドポイントが独立したルート（7ルート）
- ✅ HTTPメソッドがXMLタグで明確（`<get>`, `<post>`, `<put>`, `<delete>`）
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

## Java設定（簡略化版）

```java
@SpringBootApplication(exclude = ServletMappingAutoConfiguration.class)
public class RequestHandlingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RequestHandlingApplication.class, args);
    }
    
    @Bean
    public ServletRegistrationBean<CamelHttpTransportServlet> servletRegistrationBean() {
        ServletRegistrationBean<CamelHttpTransportServlet> registration = 
            new ServletRegistrationBean<>(new CamelHttpTransportServlet(), "/api/*");
        registration.setName("CamelServlet");
        return registration;
    }
}
```

```yaml
# application.yml
camel:
  springboot:
    name: RequestHandlingCamelContext
    xml-routes: "classpath:camel/*.xml"
```

## REST DSL vs 単一ルートの比較

| 項目 | REST DSL | 単一ルート + Choice |
|-----|----------|-------------------|
| ルート数 | 7ルート（独立） | 1ルート（統合） |
| XMLの行数 | 約100行 | 約161行 |
| 可読性 | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| 保守性 | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| 既存構成との互換性 | ✅ 維持可能 | ❌ 破壊的 |
| HTTPメソッド定義 | XMLタグ（`<get>`, `<post>`等） | `<choice>`で条件分岐 |
| パスパラメータ | 自動抽出（`{id}`） | 手動抽出（`substring`） |
| 404/405処理 | 自動 | 手動実装 |
| 新規EP追加 | 簡単（5分） | やや困難（15分） |
| Platform HTTP構成との類似性 | 高い | 低い |

## Platform HTTP構成との比較

### Platform HTTP

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

### Servlet + REST DSL

```xml
<rest path="/users">
  <get uri="/">
    <route id="get-users-route">
      <log message="ユーザー一覧取得リクエスト"/>
      <process ref="getUsersProcessor"/>
    </route>
  </get>
  
  <post uri="/">
    <route id="create-user-route">
      <log message="ユーザー作成: ${body}"/>
      <process ref="createUserProcessor"/>
    </route>
  </post>
</rest>
```

**類似点**:
- ✅ 各エンドポイントが独立したルート
- ✅ HTTPメソッドが明確
- ✅ 構造が似ている
- ✅ 404/405が自動処理

**相違点**:
- Platform HTTP: `httpMethodRestrict`パラメータで指定
- Servlet + REST DSL: XMLタグ（`<get>`, `<post>`）で指定

## 既存のREST DSL構成への影響

### ✅ 既存構成を維持可能

REST DSLを使用する限り、既存の構成を**そのまま維持**できます。

**例**: 既存のAPIがある場合

```xml
<!-- 既存のAPI -->
<rest path="/orders">
  <get uri="/">
    <route id="get-orders-route">
      <!-- ... -->
    </route>
  </get>
</rest>

<!-- 新規追加のAPI -->
<rest path="/users">
  <get uri="/">
    <route id="get-users-route">
      <!-- ... -->
    </route>
  </get>
</rest>
```

**影響なし**: 各`<rest>`ブロックは独立しており、相互に影響しません。

## エラーハンドリングの実装パターン

### パターン1: ルート内でエラー処理

```xml
<rest path="/users">
  <get uri="/">
    <route id="get-users-route">
      <doTry>
        <process ref="getUsersProcessor"/>
        <doCatch>
          <exception>java.lang.Exception</exception>
          <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
          <process ref="globalErrorProcessor"/>
        </doCatch>
      </doTry>
    </route>
  </get>
</rest>
```

**特徴**:
- ✅ 各ルートで個別にエラー処理
- ✅ きめ細かい制御が可能

### パターン2: グローバルエラーハンドラ

```xml
<errorHandler type="defaultErrorHandler">
  <redeliveryPolicy maximumRedeliveries="0"/>
  <onException>
    <exception>java.lang.Exception</exception>
    <handled><constant>true</constant></handled>
    <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
    <process ref="globalErrorProcessor"/>
  </onException>
</errorHandler>

<rest path="/users">
  <get uri="/">
    <route id="get-users-route">
      <process ref="getUsersProcessor"/>
    </route>
  </get>
</rest>
```

**特徴**:
- ✅ すべてのルートで統一的なエラー処理
- ✅ ルート定義がシンプル

## まとめ

### 質問への回答

> 正常のリクエストを受信するservletコンシューマエンドポイントが既に複数ある前提で、それらの構成を壊さず404や405などをハンドリングすることは難しいのでしょうか。

**回答**: ✅ **可能です。REST DSLを使用すれば実現できます。**

### キーポイント

1. ✅ **REST DSLを使用する**
   - `<restConfiguration component="servlet">`
   - `<rests>` / `<rest>` / `<get>`, `<post>`, `<put>`, `<delete>`

2. ✅ **各エンドポイントを独立したルートとして定義**
   - 既存のREST DSL構成と同じ構造
   - Platform HTTPの構成に近い

3. ✅ **404/405エラーは自動的に処理**
   - REST DSLが未定義のパス/メソッドをキャッチ
   - CustomErrorControllerがJSONレスポンスを生成

4. ✅ **既存構成への影響なし**
   - 各`<rest>`ブロックは独立
   - 新規追加が容易

### 推奨構成

**Servletを使用する場合は、必ずREST DSLを使用してください。**

単一ルート + Choice構成は、以下の理由で**非推奨**です：
- ❌ 既存のREST DSL構成を破壊
- ❌ 複雑で保守が困難
- ❌ 新規エンドポイント追加が困難

---

**作成日**: 2025-11-17  
**プロジェクト**: Apache Camel 4 REST API実装  
**バージョン**: 2.0.0 (訂正版)

