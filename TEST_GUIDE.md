# テストガイド

このドキュメントでは、プロジェクトのテストコードの実行方法と構成を説明します。

## テスト構成

このプロジェクトには以下のテストが含まれています：

### 1. ユニットテスト

#### UserServiceTest
- **場所**: `src/test/java/com/example/requesthandling/service/UserServiceTest.java`
- **対象**: `UserService`の全メソッド
- **テスト内容**:
  - 初期データの確認
  - ユーザー一覧取得
  - IDによるユーザー取得
  - ユーザー作成
  - ユーザー更新
  - ユーザー削除
  - 複数ユーザーの連続作成

### 2. Processor統合テスト

#### GetUsersProcessorTest
- **対象**: ユーザー一覧取得プロセッサー
- **テスト内容**: 正常なユーザー一覧取得

#### CreateUserProcessorTest
- **対象**: ユーザー作成プロセッサー
- **テスト内容**:
  - 正常なユーザー作成
  - 不正なJSON処理
  - 空のリクエストボディ処理

#### GlobalErrorProcessorTest
- **対象**: グローバルエラープロセッサー
- **テスト内容**:
  - 例外のエラーレスポンス変換
  - null例外の処理
  - HTTPヘッダーなしの処理

### 3. Camelルート統合テスト

#### CamelRoutesIntegrationTest
- **対象**: Camelルートの構成と起動
- **テスト内容**:
  - Camelコンテキストの起動確認
  - 全ルートの登録確認
  - UserServiceの注入確認
  - ルートのステータス確認
  - Camelコンポーネントの確認

### 4. REST APIエンドポイントテスト

#### RestApiEndpointTest
- **対象**: REST APIエンドポイント（E2E）
- **テスト内容**:
  - GET /api/health - ヘルスチェック
  - GET /api/users - ユーザー一覧取得
  - GET /api/users/{id} - ユーザー詳細取得
  - POST /api/users - ユーザー作成
  - PUT /api/users/{id} - ユーザー更新
  - DELETE /api/users/{id} - ユーザー削除
  - エラーケース

#### CustomErrorControllerTest
- **対象**: カスタムエラーコントローラー
- **テスト内容**:
  - 404エラー
  - 405エラー（複数パターン）
  - エラーレスポンスの構造確認

### 5. アプリケーション起動テスト

#### RequestHandlingApplicationTest
- **対象**: アプリケーション全体
- **テスト内容**:
  - Spring Bootアプリケーションの起動
  - Camelコンテキストの起動
  - 必要なBeanの登録確認

## テストの実行方法

### 全テストの実行

```bash
mvn test
```

### 特定のテストクラスの実行

```bash
# UserServiceのテスト
mvn test -Dtest=UserServiceTest

# Camelルートの統合テスト
mvn test -Dtest=CamelRoutesIntegrationTest

# REST APIエンドポイントのテスト
mvn test -Dtest=RestApiEndpointTest
```

### 特定のテストメソッドの実行

```bash
mvn test -Dtest=UserServiceTest#testGetAllUsers
```

## テスト環境の設定

### テスト用設定ファイル

テスト用の設定は`src/test/resources/application-test.yml`に定義されています：

```yaml
server:
  port: 0  # ランダムポートを使用（テスト時の競合を避ける）

camel:
  springboot:
    name: TestCamelContext
    xml-routes: "classpath:camel/*.xml"

logging:
  level:
    root: WARN
    org.apache.camel: INFO
    com.example: DEBUG
```

### JDK 21との互換性

このプロジェクトはJDK 21で動作しますが、MockitoとJDK 21には既知の互換性問題があります。`pom.xml`でMaven Surefireプラグインに以下の設定を追加しています：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>3.2.5</version>
  <configuration>
    <argLine>-Xshare:off</argLine>
  </configuration>
</plugin>
```

**注意**: JDK 17を使用すると、すべてのテストが正常に動作します。

## テスト結果の確認

テスト実行後、以下の場所に詳細なレポートが生成されます：

- `target/surefire-reports/` - テスト結果のレポート
- `target/surefire-reports/TEST-*.xml` - JUnit XMLレポート
- `target/surefire-reports/*.txt` - テキスト形式のレポート

## テストカバレッジ

### 現在のカバレッジ

- **UserService**: 100% (全メソッドをテスト)
- **Processor**: 主要なシナリオをカバー
- **REST API**: 全エンドポイントをカバー
- **エラーハンドリング**: 404、405、500エラーをカバー

### カバレッジレポートの生成

JaCoCo Maven Pluginを使用してカバレッジレポートを生成できます（オプション）：

```bash
mvn clean test jacoco:report
```

## トラブルシューティング

### 問題1: ポート競合エラー

**症状**: `Address already in use`エラーが発生する

**解決方法**:
```bash
# 使用中のポートを確認
lsof -i :8080

# プロセスを終了
kill -9 <PID>
```

### 問題2: Camelコンテキストが起動しない

**確認事項**:
1. `src/main/resources/camel/routes.xml`が存在するか
2. XMLの構文が正しいか
3. 依存関係が正しくインストールされているか

**解決方法**:
```bash
mvn clean install
```

### 問題3: テストがタイムアウトする

**解決方法**: テストクラスに`@Timeout`アノテーションを追加するか、`application-test.yml`でタイムアウト設定を調整します。

```java
@Test
@Timeout(value = 30, unit = TimeUnit.SECONDS)
void testLongRunningOperation() {
    // テストコード
}
```

## テストのベストプラクティス

### 1. テストの独立性

各テストは他のテストに依存せず、独立して実行できるように設計されています。

### 2. Given-When-Then パターン

すべてのテストは以下のパターンに従っています：

```java
@Test
void testExample() {
    // Given - テスト前の状態を設定
    User user = new User();
    
    // When - テスト対象の操作を実行
    User result = userService.createUser(user);
    
    // Then - 結果を検証
    assertNotNull(result);
    assertEquals(user.getName(), result.getName());
}
```

### 3. 明確なテスト名

テスト名は日本語の`@DisplayName`アノテーションで明確に説明されています。

### 4. エラーケースのテスト

正常系だけでなく、異常系のテストも含まれています。

## 継続的インテグレーション（CI）

このテストは以下のCI環境で実行できます：

- **GitHub Actions**
- **Jenkins**
- **GitLab CI**
- **Circle CI**

### GitHub Actions の設定例

`.github/workflows/test.yml`:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          distribution: 'adopt'
      - name: Run tests
        run: mvn test
```

## まとめ

このプロジェクトには、以下の階層のテストが含まれています：

1. **ユニットテスト**: 個々のクラスの動作を検証
2. **統合テスト**: 複数のコンポーネントの連携を検証
3. **E2Eテスト**: エンドポイント経由での動作を検証

すべてのテストを実行することで、アプリケーションの品質と安定性を確保できます。

