# API使用例

このドキュメントでは、Request Handling APIの各エンドポイントの使用例を紹介します。

## 目次

1. [正常なリクエスト](#正常なリクエスト)
2. [エラーケース](#エラーケース)
3. [レスポンス形式](#レスポンス形式)

---

## 正常なリクエスト

### 1. ヘルスチェック

アプリケーションの稼働状態を確認します。

**リクエスト:**
```bash
curl -X GET http://localhost:8080/api/health
```

**レスポンス例:**
```json
{
  "status": "UP",
  "message": "アプリケーションは正常に稼働しています",
  "data": "2025-11-10T10:30:00"
}
```

---

### 2. ユーザー一覧取得

全ユーザーの一覧を取得します。

**リクエスト:**
```bash
curl -X GET http://localhost:8080/api/users
```

**レスポンス例:**
```json
{
  "status": "success",
  "message": "ユーザー一覧を取得しました",
  "data": [
    {
      "id": "1",
      "name": "user1",
      "email": "user1@example.com"
    },
    {
      "id": "2",
      "name": "user2",
      "email": "user2@example.com"
    },
    {
      "id": "3",
      "name": "user3",
      "email": "user3@example.com"
    }
  ]
}
```

---

### 3. ユーザー詳細取得

特定のユーザーの詳細情報を取得します。

**リクエスト:**
```bash
curl -X GET http://localhost:8080/api/users/123
```

**レスポンス例:**
```json
{
  "status": "success",
  "message": "ユーザー詳細を取得しました",
  "data": {
    "id": "1",
    "name": "user1",
    "email": "user1@example.com"
  }
}
```

---

### 4. ユーザー作成

新規ユーザーを作成します。

**リクエスト:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "田中太郎",
    "email": "tanaka@example.com"
  }'
```

**レスポンス例 (HTTP 201 Created):**
```json
{
  "status": "success",
  "message": "ユーザーを作成しました (ID: 4)",
  "data": {
    "id": "4",
    "name": "田中太郎",
    "email": "tanaka@example.com"
  }
}
```

**注意**: ユーザーIDは自動採番されます。

---

### 5. ユーザー更新

既存ユーザーの情報を更新します。

**リクエスト:**
```bash
curl -X PUT http://localhost:8080/api/users/123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "田中花子",
    "email": "hanako@example.com"
  }'
```

**レスポンス例:**
```json
{
  "status": "success",
  "message": "ユーザーを更新しました",
  "data": {
    "id": "1",
    "name": "田中花子",
    "email": "hanako@example.com"
  }
}
```

---

### 6. ユーザー削除

指定されたユーザーを削除します。

**リクエスト:**
```bash
curl -X DELETE http://localhost:8080/api/users/123
```

**レスポンス例:**
```json
{
  "status": "success",
  "message": "ユーザーを削除しました",
  "data": "Deleted User ID: 123"
}
```

---

## エラーケース

### 404エラー - リソースが見つからない

存在しないパスにアクセスした場合のレスポンスです。

**リクエスト:**
```bash
curl -X GET http://localhost:8080/api/nonexistent
```

**レスポンス例 (HTTP 404 Not Found):**
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

---

### 405エラー - メソッドが許可されていない

エンドポイントが対応していないHTTPメソッドでアクセスした場合のレスポンスです。

**リクエスト:**
```bash
curl -X PATCH http://localhost:8080/api/users/123 \
  -H "Content-Type: application/json" \
  -d '{}'
```

**レスポンス例 (HTTP 405 Method Not Allowed):**
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

---

### 500エラー - 内部サーバーエラー

サーバー側で予期しないエラーが発生した場合のレスポンスです。

**テスト用エンドポイント:**
```bash
curl -X GET http://localhost:8080/api/test/error | jq
```

**レスポンス例 (HTTP 500 Internal Server Error):**
```json
{
  "code": 500,
  "message": "内部サーバーエラーが発生しました",
  "details": "これはテスト用のエラーです",
  "timestamp": "2025-11-11T10:36:20.849094",
  "path": "/api/test/error",
  "method": "GET"
}
```

---

## レスポンス形式

### 正常レスポンス

正常なリクエストの場合、以下の形式でレスポンスが返されます。

```json
{
  "status": "成功ステータス",
  "message": "処理結果メッセージ",
  "data": "返却データ（オブジェクト、配列、文字列など）"
}
```

### エラーレスポンス

エラーが発生した場合、以下の形式でレスポンスが返されます。

```json
{
  "code": "HTTPステータスコード",
  "message": "エラーメッセージ",
  "details": "エラーの詳細情報",
  "timestamp": "エラー発生日時（ISO 8601形式）",
  "path": "リクエストパス",
  "method": "HTTPメソッド"
}
```

---

## テストスクリプトの実行

提供されているテストスクリプトを使用して、すべてのエンドポイントを一度にテストできます。

```bash
# スクリプトに実行権限を付与（初回のみ）
chmod +x test-api.sh

# テストを実行
./test-api.sh
```

**注意:** `jq`コマンドがインストールされている必要があります。インストールされていない場合は、以下のコマンドでインストールしてください。

```bash
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# CentOS/RHEL
sudo yum install jq
```

---

## HTTPクライアントツールの使用

### Postman

1. Postmanを開く
2. 新しいリクエストを作成
3. メソッドとURLを設定
4. 必要に応じてヘッダーとボディを設定
5. 「Send」をクリック

### HTTPie

より人間に優しいHTTPクライアント：

```bash
# GET リクエスト
http GET http://localhost:8080/api/users

# POST リクエスト
http POST http://localhost:8080/api/users \
  name="田中太郎" \
  email="tanaka@example.com"

# PUT リクエスト
http PUT http://localhost:8080/api/users/123 \
  name="田中花子" \
  email="hanako@example.com"

# DELETE リクエスト
http DELETE http://localhost:8080/api/users/123
```

---

## トラブルシューティング

### 接続できない場合

1. アプリケーションが起動していることを確認
   ```bash
   # ログを確認
   mvn spring-boot:run
   ```

2. ポート8080が使用中でないことを確認
   ```bash
   lsof -i :8080
   ```

3. ファイアウォールの設定を確認

### JSONレスポンスが文字化けする場合

curlコマンドに`-H "Accept-Charset: UTF-8"`を追加してください。

```bash
curl -X GET http://localhost:8080/api/users \
  -H "Accept-Charset: UTF-8"
```

---

## 参考情報

- [Apache Camel Documentation](https://camel.apache.org/manual/latest/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Undertow Documentation](https://undertow.io/undertow-docs/undertow-docs-2.2.0/)

