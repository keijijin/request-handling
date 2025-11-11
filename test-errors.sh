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

