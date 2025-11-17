# カスタムエラーハンドリングガイド

このガイドでは、404（Not Found）および405（Method Not Allowed）エラーをカスタマイズする方法を説明します。

## 目次

1. [エラーハンドリングの仕組み](#エラーハンドリングの仕組み)
2. [エラーメッセージのカスタマイズ](#エラーメッセージのカスタマイズ)
3. [エラーレスポンス形式のカスタマイズ](#エラーレスポンス形式のカスタマイズ)
4. [Camelルートレベルのエラーハンドリング](#camelルートレベルのエラーハンドリング)
5. [テスト方法](#テスト方法)

---

## エラーハンドリングの仕組み

本アプリケーションには、2階層のエラーハンドリングがあります：

### レベル1: Servletレベル（404, 405など）

**対象**: Spring BootのDispatcherServletで処理される前のエラー
- 存在しないURLへのアクセス（404）
- 定義されていないHTTPメソッドの使用（405）
- サーブレットレベルの例外

**ハンドラー**: `CustomErrorController`

### レベル2: Camelルートレベル

**対象**: Camelルート実行中に発生する例外
- ビジネスロジックの例外
- データ処理エラー
- 外部サービス呼び出しエラー

**ハンドラー**: 各ルートの`doCatch`ブロックと`GlobalErrorProcessor`

---

## エラーメッセージのカスタマイズ

### 方法1: application.ymlで設定（推奨）

最もシンプルな方法です。`src/main/resources/application.yml`を編集します。

```yaml
# カスタムエラーメッセージの設定
error:
  messages:
    404: "指定されたリソースが見つかりません"
    405: "許可されていないHTTPメソッドです"
    500: "内部サーバーエラーが発生しました"
    default: "エラーが発生しました"
```

#### カスタマイズ例

**ビジネス向けの丁寧な表現**:
```yaml
error:
  messages:
    404: "お探しのページは見つかりませんでした。URLをご確認ください。"
    405: "このリクエスト方法はサポートされておりません。"
    500: "サーバー側で問題が発生しました。しばらくしてから再度お試しください。"
    default: "予期しないエラーが発生しました。"
```

**開発者向けの技術的な表現**:
```yaml
error:
  messages:
    404: "Resource not found. Check the endpoint path."
    405: "HTTP method not allowed for this endpoint."
    500: "Internal server error occurred. Check logs for details."
    default: "An error occurred during request processing."
```

**多言語対応の準備**:
```yaml
error:
  messages:
    404: "リソースが見つかりません / Resource not found"
    405: "メソッドが許可されていません / Method not allowed"
    500: "サーバーエラー / Internal server error"
    default: "エラー / Error"
```

### 方法2: Javaコードでカスタマイズ（高度）

より複雑なロジックが必要な場合、`CustomErrorController.java`を直接編集します。

**ファイル**: `src/main/java/com/example/requesthandling/controller/CustomErrorController.java`

#### 例1: ステータスコード別に詳細なメッセージを追加

```java
private String getDetailMessage(int statusCode, String path, String method) {
    switch (statusCode) {
        case 404:
            return String.format(
                "パス '%s' は存在しません。URLを確認してください。利用可能なエンドポイント一覧は /api/users をご確認ください。",
                path
            );
        case 405:
            return String.format(
                "パス '%s' に対して、メソッド '%s' は許可されていません。GET, POST, PUT, DELETE のいずれかをご使用ください。",
                path, method
            );
        case 500:
            return "サーバー内部でエラーが発生しました。管理者に連絡してください。";
        default:
            return String.format("HTTPステータスコード %d のエラーが発生しました。", statusCode);
    }
}
```

#### 例2: リクエスト情報に基づく動的なメッセージ

```java
@RequestMapping
public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
    Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    String method = request.getMethod();
    Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    String path = requestUri != null ? requestUri.toString() : "不明";
    
    // カスタムメッセージロジック
    String customMessage = errorMessageProperties.getMessage(statusCode);
    String detailMessage = getDetailMessage(statusCode, path, method);
    
    // APIエンドポイントへのアクセスの場合、より具体的なヘルプを提供
    if (path.startsWith("/api/") && statusCode == 404) {
        detailMessage += " 利用可能なAPIは /api/users, /api/health です。";
    }
    
    ErrorResponse errorResponse = ErrorResponse.builder()
            .code(statusCode)
            .message(customMessage)
            .details(detailMessage)
            .timestamp(LocalDateTime.now().toString())
            .path(path)
            .method(method)
            .build();
    
    HttpStatus httpStatus = HttpStatus.resolve(statusCode);
    if (httpStatus == null) {
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return new ResponseEntity<>(errorResponse, httpStatus);
}
```

#### 例3: 環境別のメッセージ（本番環境では詳細を隠す）

```java
@Value("${spring.profiles.active:default}")
private String activeProfile;

private String getDetailMessage(int statusCode, String path, String method) {
    // 本番環境では詳細情報を隠す
    if ("production".equals(activeProfile)) {
        switch (statusCode) {
            case 404:
                return "指定されたリソースが見つかりません。";
            case 405:
                return "許可されていないリクエストです。";
            case 500:
                return "エラーが発生しました。";
            default:
                return "リクエストを処理できませんでした。";
        }
    }
    
    // 開発環境では詳細情報を表示
    switch (statusCode) {
        case 404:
            return String.format(
                "パス '%s' は存在しません。URLを確認してください。",
                path
            );
        case 405:
            return String.format(
                "パス '%s' に対して、メソッド '%s' は許可されていません。",
                path, method
            );
        case 500:
            return "サーバー内部でエラーが発生しました。ログを確認してください。";
        default:
            return String.format("HTTPステータスコード %d のエラーが発生しました。", statusCode);
    }
}
```

---

## エラーレスポンス形式のカスタマイズ

### 現在のレスポンス形式

```json
{
  "code": 404,
  "message": "指定されたリソースが見つかりません",
  "details": "パス '/api/nonexistent' は存在しません。URLを確認してください。",
  "timestamp": "2025-11-11T09:55:00.123456",
  "path": "/api/nonexistent",
  "method": "GET"
}
```

### カスタマイズ方法

`src/main/java/com/example/requesthandling/model/ErrorResponse.java`を編集します。

#### 例1: エラーIDとサポートURLを追加

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int code;
    private String message;
    private String details;
    private String timestamp;
    private String path;
    private String method;
    
    // 追加フィールド
    private String errorId;      // エラー追跡用のユニークID
    private String supportUrl;   // サポートページのURL
    private String suggestion;   // ユーザーへの提案
}
```

`CustomErrorController.java`で設定：

```java
ErrorResponse errorResponse = ErrorResponse.builder()
        .code(statusCode)
        .message(customMessage)
        .details(detailMessage)
        .timestamp(LocalDateTime.now().toString())
        .path(path)
        .method(method)
        .errorId(UUID.randomUUID().toString())  // ユニークなエラーID
        .supportUrl("https://support.example.com/api-errors")
        .suggestion(getSuggestion(statusCode))
        .build();

private String getSuggestion(int statusCode) {
    switch (statusCode) {
        case 404:
            return "URLが正しいか確認してください。または /api/users にアクセスしてください。";
        case 405:
            return "許可されているHTTPメソッドを確認してください。";
        default:
            return null;
    }
}
```

**出力例**:
```json
{
  "code": 404,
  "message": "指定されたリソースが見つかりません",
  "details": "パス '/api/nonexistent' は存在しません。",
  "timestamp": "2025-11-11T09:55:00.123456",
  "path": "/api/nonexistent",
  "method": "GET",
  "errorId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "supportUrl": "https://support.example.com/api-errors",
  "suggestion": "URLが正しいか確認してください。または /api/users にアクセスしてください。"
}
```

#### 例2: RFC 7807 (Problem Details) 形式に準拠

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @JsonProperty("type")
    private String type;        // エラータイプのURI
    
    @JsonProperty("title")
    private String title;       // 短い説明
    
    @JsonProperty("status")
    private int status;         // HTTPステータスコード
    
    @JsonProperty("detail")
    private String detail;      // 詳細説明
    
    @JsonProperty("instance")
    private String instance;    // 問題が発生した特定のリクエスト
    
    @JsonProperty("timestamp")
    private String timestamp;
}
```

**出力例**:
```json
{
  "type": "https://api.example.com/errors/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "パス '/api/nonexistent' は存在しません。",
  "instance": "/api/nonexistent",
  "timestamp": "2025-11-11T09:55:00.123456"
}
```

---

## Camelルートレベルのエラーハンドリング

Camelルート内で発生するエラー（ビジネスロジックの例外など）は、`GlobalErrorProcessor`で処理されます。

### カスタマイズ方法

`src/main/java/com/example/requesthandling/processor/GlobalErrorProcessor.java`を編集します。

#### 現在の実装

```java
@Component("globalErrorProcessor")
public class GlobalErrorProcessor implements Processor {
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(500)
                .message("内部サーバーエラーが発生しました")
                .details(exception != null ? exception.getMessage() : "不明なエラー")
                .timestamp(LocalDateTime.now().toString())
                .path(exchange.getIn().getHeader(Exchange.HTTP_URI, String.class))
                .method(exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class))
                .build();
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        exchange.getMessage().setBody(jsonResponse);
    }
}
```

#### 例1: 例外タイプ別のエラーメッセージ

```java
@Override
public void process(Exchange exchange) throws Exception {
    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
    
    int statusCode = 500;
    String message = "内部サーバーエラーが発生しました";
    String details = exception != null ? exception.getMessage() : "不明なエラー";
    
    // 例外タイプ別の処理
    if (exception instanceof IllegalArgumentException) {
        statusCode = 400;
        message = "不正なリクエストです";
        details = exception.getMessage();
    } else if (exception instanceof NullPointerException) {
        message = "必要なデータが見つかりません";
        details = "処理に必要なデータが不足しています";
    } else if (exception instanceof JsonProcessingException) {
        statusCode = 400;
        message = "JSON解析エラー";
        details = "リクエストボディのJSON形式が正しくありません";
    }
    
    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
    
    ErrorResponse errorResponse = ErrorResponse.builder()
            .code(statusCode)
            .message(message)
            .details(details)
            .timestamp(LocalDateTime.now().toString())
            .path(exchange.getIn().getHeader(Exchange.HTTP_URI, String.class))
            .method(exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class))
            .build();
    
    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
    exchange.getMessage().setBody(jsonResponse);
}
```

#### 例2: ログ出力を追加

```java
@Component("globalErrorProcessor")
public class GlobalErrorProcessor implements Processor {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorProcessor.class);
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String path = exchange.getIn().getHeader(Exchange.HTTP_URI, String.class);
        String method = exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
        
        // エラーログ出力
        log.error("エラーが発生しました - Path: {}, Method: {}", path, method, exception);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(500)
                .message("内部サーバーエラーが発生しました")
                .details(exception != null ? exception.getMessage() : "不明なエラー")
                .timestamp(LocalDateTime.now().toString())
                .path(path)
                .method(method)
                .build();
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        exchange.getMessage().setBody(jsonResponse);
    }
}
```

---

## テスト方法

### 1. 404エラーのテスト

**存在しないエンドポイントへのアクセス**:

```bash
curl -X GET http://localhost:8080/api/nonexistent | jq
```

**期待される出力**:
```json
{
  "code": 404,
  "message": "指定されたリソースが見つかりません",
  "details": "パス '/api/nonexistent' は存在しません。URLを確認してください。",
  "timestamp": "2025-11-11T09:55:00.123456",
  "path": "/api/nonexistent",
  "method": "GET"
}
```

### 2. 405エラーのテスト

**許可されていないHTTPメソッドの使用**:

#### PATCHメソッド（定義されていない）

```bash
curl -X PATCH http://localhost:8080/api/users/1 | jq
```

**期待される出力**:
```json
{
  "code": 405,
  "message": "許可されていないHTTPメソッドです",
  "details": "パス '/api/users/1' に対して、メソッド 'PATCH' は許可されていません。",
  "timestamp": "2025-11-11T10:29:00.847151",
  "path": "/api/users/1",
  "method": "PATCH"
}
```

#### HEADメソッド（定義されていない）

```bash
# HEADリクエストには -I オプションを使用（推奨）
curl -I http://localhost:8080/api/users
```

**期待される出力**:
```
HTTP/1.1 405 Method Not Allowed
Connection: keep-alive
Transfer-Encoding: chunked
Content-Type: application/json
Date: Tue, 11 Nov 2025 01:28:49 GMT
```

**注意**: `-I` オプションはヘッダーのみを表示します。ボディを含む完全なレスポンスを見たい場合は `-X HEAD` を使用できますが、curlから警告が表示されます。

#### PUTメソッド（IDなしのパス - 定義されていない）

```bash
# PUT は /api/users/{id} のみで許可されている
curl -X PUT http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"test"}' | jq
```

**期待される出力**:
```json
{
  "code": 405,
  "message": "許可されていないHTTPメソッドです",
  "details": "パス '/api/users' に対して、メソッド 'PUT' は許可されていません。",
  "timestamp": "2025-11-11T10:29:26.128514",
  "path": "/api/users",
  "method": "PUT"
}
```

#### OPTIONSメソッドについて

```bash
curl -X OPTIONS http://localhost:8080/api/users | jq
```

**注意**: OPTIONSリクエストはCORSプリフライトリクエストとして処理される場合があり、レスポンスボディが返らないことがあります。これは`enableCORS(true)`設定により、Camelが自動的にCORS対応を行うためです。

### 3. 500エラーのテスト（Camelルートレベル）

意図的にエラーを発生させてテストする場合、テスト用のエンドポイントを追加できます。

**Step 1**: `TestErrorProcessor.java`を作成：

```java
@Component
public class TestErrorProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        throw new RuntimeException("これはテスト用のエラーです");
    }
}
```

**Step 2**: `routes.xml`に追加：

```xml
<!-- テストエラールート -->
<route id="test-error-route">
    <from uri="platform-http:/api/test/error?httpMethodRestrict=GET"/>
    <log message="テストエラーを発生させます"/>
    <doTry>
        <process ref="testErrorProcessor"/>
        <doCatch>
            <exception>java.lang.Exception</exception>
            <setHeader name="CamelHttpResponseCode"><constant>500</constant></setHeader>
            <setHeader name="Content-Type"><constant>application/json</constant></setHeader>
            <process ref="globalErrorProcessor"/>
        </doCatch>
    </doTry>
</route>
```

```bash
curl -X GET http://localhost:8080/api/test/error | jq
```

**期待される出力**:
```json
{
  "code": 500,
  "message": "内部サーバーエラーが発生しました",
  "details": "これはテスト用のエラーです",
  "timestamp": "2025-11-11T09:55:00.123456",
  "path": "/api/test/error",
  "method": "GET"
}
```

### 4. 包括的なテストスクリプト

`test-errors.sh`を作成：

```bash
#!/bin/bash

echo "=== 404エラーテスト: 存在しないパス ==="
curl -X GET http://localhost:8080/api/nonexistent | jq
echo ""

echo "=== 404エラーテスト: 存在しないユーザーID ==="
curl -X GET http://localhost:8080/api/users/999 | jq
echo ""

echo "=== 405エラーテスト: PATCH メソッド ==="
curl -X PATCH http://localhost:8080/api/users/1 | jq
echo ""

echo "=== 405エラーテスト: HEAD メソッド ==="
curl -I http://localhost:8080/api/users
echo ""

echo "=== 405エラーテスト: PUT メソッド (IDなし) ==="
curl -X PUT http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"test"}' | jq
echo ""

echo "=== 正常系: ユーザー一覧取得 ==="
curl -X GET http://localhost:8080/api/users | jq
echo ""

echo "=== 正常系: ユーザー作成 ==="
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"テストユーザー","email":"test@example.com"}' | jq
echo ""

echo "=== 正常系: ヘルスチェック ==="
curl -X GET http://localhost:8080/api/health | jq
echo ""
```

実行：
```bash
chmod +x test-errors.sh
./test-errors.sh
```

**期待される動作**:
- 404エラー: カスタムメッセージが表示される
- 405エラー: 各HTTPメソッドに対してカスタムメッセージが表示される
- 正常系: 適切なレスポンスが返される
- データ永続化: POSTしたデータが保持される

---

## まとめ

### カスタマイズのレベル

| レベル | 方法 | 難易度 | 用途 |
|--------|------|--------|------|
| **レベル1** | `application.yml`の編集 | ★☆☆ | メッセージの変更のみ |
| **レベル2** | `CustomErrorController`の編集 | ★★☆ | ロジックの追加、詳細なカスタマイズ |
| **レベル3** | `ErrorResponse`モデルの変更 | ★★★ | レスポンス形式の完全な変更 |

### ベストプラクティス

1. **シンプルなカスタマイズは設定ファイルで**: メッセージだけ変えたい場合は`application.yml`を使用
2. **環境別の設定**: 本番環境では詳細情報を隠す
3. **ログ出力**: エラーは必ずログに記録する
4. **ユーザーフレンドリー**: エラーメッセージは具体的で理解しやすく
5. **一貫性**: すべてのエラーレスポンスで同じ形式を使用
6. **セキュリティ**: 本番環境ではシステム内部の情報を露出しない

### 参考リンク

- [Spring Boot Error Handling](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.spring-mvc.error-handling)
- [RFC 7807: Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc7807)
- [Apache Camel Error Handler](https://camel.apache.org/manual/error-handler.html)

