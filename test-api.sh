#!/bin/bash

# APIテストスクリプト
# 使い方: ./test-api.sh

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Request Handling API テスト"
echo "=========================================="
echo ""

# カラー設定
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ヘルスチェック
echo -e "${YELLOW}1. ヘルスチェック${NC}"
echo "GET ${BASE_URL}/api/health"
curl -s -X GET "${BASE_URL}/api/health" | jq .
echo ""
echo ""

# ユーザー一覧取得
echo -e "${YELLOW}2. ユーザー一覧取得${NC}"
echo "GET ${BASE_URL}/api/users"
curl -s -X GET "${BASE_URL}/api/users" | jq .
echo ""
echo ""

# ユーザー詳細取得
echo -e "${YELLOW}3. ユーザー詳細取得${NC}"
echo "GET ${BASE_URL}/api/users/1"
curl -s -X GET "${BASE_URL}/api/users/1" | jq .
echo ""
echo ""

# ユーザー作成
echo -e "${YELLOW}4. ユーザー作成${NC}"
echo "POST ${BASE_URL}/api/users"
curl -s -X POST "${BASE_URL}/api/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"田中太郎","email":"tanaka@example.com"}' | jq .
echo ""
echo ""

# ユーザー更新
echo -e "${YELLOW}5. ユーザー更新${NC}"
echo "PUT ${BASE_URL}/api/users/4"
curl -s -X PUT "${BASE_URL}/api/users/4" \
  -H "Content-Type: application/json" \
  -d '{"name":"田中花子","email":"hanako@example.com"}' | jq .
echo ""
echo ""

# ユーザー削除
echo -e "${YELLOW}6. ユーザー削除${NC}"
echo "DELETE ${BASE_URL}/api/users/4"
curl -s -X DELETE "${BASE_URL}/api/users/4" | jq .
echo ""
echo ""

echo "=========================================="
echo "エラーケースのテスト"
echo "=========================================="
echo ""

# 404エラー（存在しないパス）
echo -e "${RED}7. 404エラー（存在しないパス）${NC}"
echo "GET ${BASE_URL}/api/nonexistent"
curl -s -X GET "${BASE_URL}/api/nonexistent" | jq .
echo ""
echo ""

# 405エラー（許可されていないメソッド）
echo -e "${RED}8. 405エラー（許可されていないメソッド）${NC}"
echo "PATCH ${BASE_URL}/api/users/1"
curl -s -X PATCH "${BASE_URL}/api/users/1" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
echo ""
echo ""

echo -e "${GREEN}テスト完了${NC}"

