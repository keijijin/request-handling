package com.example.requesthandling.route;

import com.example.requesthandling.service.UserService;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Camelルートの統合テスト
 */
@SpringBootTest
@DisplayName("Camelルートの統合テスト")
class CamelRoutesIntegrationTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 各テスト前にCamelコンテキストが起動していることを確認
        assertNotNull(camelContext);
        assertTrue(camelContext.getStatus().isStarted());
    }

    @Test
    @DisplayName("Camelコンテキストが正常に起動している")
    void testCamelContextStarted() {
        // Then
        assertNotNull(camelContext);
        assertTrue(camelContext.getStatus().isStarted());
        assertTrue(camelContext.getRoutes().size() > 0);
    }

    @Test
    @DisplayName("全ルートが登録されている")
    void testAllRoutesRegistered() {
        // Then
        long routeCount = camelContext.getRoutes().size();
        assertTrue(routeCount >= 1, "期待されるルート数: 1以上, 実際: " + routeCount);

        // 各ルートIDの確認
        assertTrue(camelContext.getRoute("api-route") != null);
    }

    @Test
    @DisplayName("UserServiceが正しく注入されている")
    void testUserServiceInjection() {
        // Then
        assertNotNull(userService);
        assertTrue(userService.getUserCount() >= 3);
    }

    @Test
    @DisplayName("ルートのステータスが全てStartedである")
    void testAllRoutesStarted() {
        // Then
        camelContext.getRoutes().forEach(route -> {
            assertTrue(camelContext.getRouteController().getRouteStatus(route.getRouteId()).isStarted(),
                    "ルート " + route.getRouteId() + " が起動していません");
        });
    }

    @Test
    @DisplayName("Camelコンポーネントが正しく登録されている")
    void testCamelComponents() {
        // Then
        assertNotNull(camelContext.getComponent("servlet"));
        assertNotNull(camelContext.getComponent("direct"));
    }
}

