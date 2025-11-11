package com.example.requesthandling.route;

import com.example.requesthandling.processor.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * REST API設定とルート定義
 * XML IO DSLの制限により、REST設定はJavaで記述
 */
@Component
public class RestApiConfiguration extends RouteBuilder {

    @Autowired
    private GetUsersProcessor getUsersProcessor;

    @Autowired
    private GetUserByIdProcessor getUserByIdProcessor;

    @Autowired
    private CreateUserProcessor createUserProcessor;

    @Autowired
    private UpdateUserProcessor updateUserProcessor;

    @Autowired
    private DeleteUserProcessor deleteUserProcessor;

    @Autowired
    private HealthCheckProcessor healthCheckProcessor;

    @Autowired
    private GlobalErrorProcessor globalErrorProcessor;

    @Override
    public void configure() throws Exception {

        // REST設定
        restConfiguration()
                .component("servlet")
                .bindingMode(RestBindingMode.off)
                .dataFormatProperty("prettyPrint", "true")
                .enableCORS(true)
                .contextPath("/api");

        // REST APIエンドポイント定義
        rest("/users")
                .description("ユーザー管理API")
                
                // GET /api/users - ユーザー一覧取得
                .get()
                    .description("ユーザー一覧を取得")
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .to("direct:getUsers")
                
                // GET /api/users/{id} - ユーザー詳細取得
                .get("/{id}")
                    .description("ユーザー詳細を取得")
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .to("direct:getUserById")
                
                // POST /api/users - ユーザー作成
                .post()
                    .description("新規ユーザーを作成")
                    .consumes(MediaType.APPLICATION_JSON_VALUE)
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .to("direct:createUser")
                
                // PUT /api/users/{id} - ユーザー更新
                .put("/{id}")
                    .description("ユーザー情報を更新")
                    .consumes(MediaType.APPLICATION_JSON_VALUE)
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .to("direct:updateUser")
                
                // DELETE /api/users/{id} - ユーザー削除
                .delete("/{id}")
                    .description("ユーザーを削除")
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .to("direct:deleteUser");

        rest("/health")
                .description("ヘルスチェックAPI")
                .get()
                    .description("アプリケーションの状態を確認")
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .to("direct:healthCheck");

        rest("/test")
                .description("テスト用API")
                .get("/error")
                    .description("500エラーをテストするためのエンドポイント")
                    .produces(MediaType.APPLICATION_JSON_VALUE)
                    .to("direct:testError");

        // ルート実装
        
        from("direct:getUsers")
                .routeId("get-users-route")
                .log("ユーザー一覧取得リクエスト")
                .doTry()
                    .process(getUsersProcessor)
                .doCatch(Exception.class)
                    .setHeader("CamelHttpResponseCode", constant(500))
                    .process(globalErrorProcessor)
                .end();

        from("direct:getUserById")
                .routeId("get-user-by-id-route")
                .log("ユーザー詳細取得リクエスト: ID=${header.id}")
                .doTry()
                    .process(getUserByIdProcessor)
                .doCatch(Exception.class)
                    .setHeader("CamelHttpResponseCode", constant(500))
                    .process(globalErrorProcessor)
                .end();

        from("direct:createUser")
                .routeId("create-user-route")
                .log("ユーザー作成リクエスト: ${body}")
                .doTry()
                    .process(createUserProcessor)
                .doCatch(Exception.class)
                    .setHeader("CamelHttpResponseCode", constant(500))
                    .process(globalErrorProcessor)
                .end();

        from("direct:updateUser")
                .routeId("update-user-route")
                .log("ユーザー更新リクエスト: ID=${header.id}, Body=${body}")
                .doTry()
                    .process(updateUserProcessor)
                .doCatch(Exception.class)
                    .setHeader("CamelHttpResponseCode", constant(500))
                    .process(globalErrorProcessor)
                .end();

        from("direct:deleteUser")
                .routeId("delete-user-route")
                .log("ユーザー削除リクエスト: ID=${header.id}")
                .doTry()
                    .process(deleteUserProcessor)
                .doCatch(Exception.class)
                    .setHeader("CamelHttpResponseCode", constant(500))
                    .process(globalErrorProcessor)
                .end();

        from("direct:healthCheck")
                .routeId("health-check-route")
                .log("ヘルスチェックリクエスト")
                .doTry()
                    .process(healthCheckProcessor)
                .doCatch(Exception.class)
                    .setHeader("CamelHttpResponseCode", constant(500))
                    .process(globalErrorProcessor)
                .end();

        from("direct:testError")
                .routeId("test-error-route")
                .log("テストエラーを発生させます")
                .doTry()
                    .process(exchange -> {
                        throw new RuntimeException("これはテスト用のエラーです");
                    })
                .doCatch(Exception.class)
                    .setHeader("CamelHttpResponseCode", constant(500))
                    .process(globalErrorProcessor)
                .end();
    }
}

