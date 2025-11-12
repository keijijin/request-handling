package com.example.requesthandling.processor;

import com.example.requesthandling.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalErrorProcessor の統合テスト
 */
@SpringBootTest
@DisplayName("グローバルエラープロセッサーのテスト")
class GlobalErrorProcessorTest {

    @Autowired
    private GlobalErrorProcessor processor;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CamelContext camelContext;

    private Exchange exchange;

    @BeforeEach
    void setUp() {
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    @DisplayName("例外を正しくエラーレスポンスに変換できる")
    void testProcessWithException() throws Exception {
        // Given
        Exception testException = new RuntimeException("テストエラーメッセージ");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);
        exchange.getMessage().setHeader(Exchange.HTTP_URI, "/api/test");
        exchange.getMessage().setHeader(Exchange.HTTP_METHOD, "GET");

        // When
        processor.process(exchange);

        // Then
        String responseBody = exchange.getMessage().getBody(String.class);

        assertNotNull(responseBody);

        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertEquals(500, errorResponse.getCode());
        assertEquals("内部サーバーエラーが発生しました", errorResponse.getMessage());
        assertEquals("テストエラーメッセージ", errorResponse.getDetails());
        assertEquals("/api/test", errorResponse.getPath());
        assertEquals("GET", errorResponse.getMethod());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("例外がnullの場合でも正常に動作する")
    void testProcessWithNullException() throws Exception {
        // Given
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, null);
        exchange.getMessage().setHeader(Exchange.HTTP_URI, "/api/test");
        exchange.getMessage().setHeader(Exchange.HTTP_METHOD, "POST");

        // When
        processor.process(exchange);

        // Then
        String responseBody = exchange.getMessage().getBody(String.class);
        assertNotNull(responseBody);

        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertEquals(500, errorResponse.getCode());
        assertEquals("不明なエラー", errorResponse.getDetails());
    }

    @Test
    @DisplayName("HTTPヘッダーがない場合でも正常に動作する")
    void testProcessWithoutHttpHeaders() throws Exception {
        // Given
        Exception testException = new RuntimeException("エラー");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, testException);

        // When
        processor.process(exchange);

        // Then
        String responseBody = exchange.getMessage().getBody(String.class);
        assertNotNull(responseBody);

        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertEquals(500, errorResponse.getCode());
        assertNull(errorResponse.getPath());
        assertNull(errorResponse.getMethod());
    }
}

