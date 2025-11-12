package com.example.requesthandling;

import org.apache.camel.CamelContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * アプリケーション起動テスト
 */
@SpringBootTest
@DisplayName("アプリケーション起動テスト")
class RequestHandlingApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CamelContext camelContext;

    @Test
    @DisplayName("Spring Bootアプリケーションが正常に起動する")
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    @DisplayName("Camelコンテキストが正常に起動している")
    void camelContextLoads() {
        assertNotNull(camelContext);
        assertTrue(camelContext.getStatus().isStarted());
    }

    @Test
    @DisplayName("必要なBeanが全て登録されている")
    void testRequiredBeansPresent() {
        // Processors
        assertTrue(applicationContext.containsBean("getUsersProcessor"));
        assertTrue(applicationContext.containsBean("createUserProcessor"));
        assertTrue(applicationContext.containsBean("getUserByIdProcessor"));
        assertTrue(applicationContext.containsBean("updateUserProcessor"));
        assertTrue(applicationContext.containsBean("deleteUserProcessor"));
        assertTrue(applicationContext.containsBean("healthCheckProcessor"));
        assertTrue(applicationContext.containsBean("globalErrorProcessor"));
        assertTrue(applicationContext.containsBean("testErrorProcessor"));

        // Service
        assertTrue(applicationContext.containsBean("userService"));

        // Controllers
        assertTrue(applicationContext.containsBean("customErrorController"));
    }
}

