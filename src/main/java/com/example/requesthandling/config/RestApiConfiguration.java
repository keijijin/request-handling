package com.example.requesthandling.config;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * REST API Configuration
 * Servlet REST DSLの設定とエンドポイント定義
 */
@Configuration
public class RestApiConfiguration {

    /**
     * REST設定とエンドポイント定義
     */
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

