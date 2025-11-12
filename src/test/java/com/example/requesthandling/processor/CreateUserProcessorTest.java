package com.example.requesthandling.processor;

import com.example.requesthandling.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CreateUserProcessor の統合テスト
 */
@SpringBootTest
@DisplayName("ユーザー作成プロセッサーのテスト")
class CreateUserProcessorTest {

    @Autowired
    private CreateUserProcessor processor;

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
    @DisplayName("ユーザーを正常に作成できる")
    void testProcessSuccess() throws Exception {
        // Given
        String requestBody = "{\"name\":\"newuser\",\"email\":\"newuser@example.com\"}";
        exchange.getMessage().setBody(requestBody);

        // When
        processor.process(exchange);

        // Then
        String responseBody = exchange.getMessage().getBody(String.class);
        String contentType = exchange.getMessage().getHeader(Exchange.CONTENT_TYPE, String.class);
        Integer httpResponseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

        assertNotNull(responseBody);
        assertEquals(MediaType.APPLICATION_JSON_VALUE, contentType);
        assertEquals(201, httpResponseCode);

        ApiResponse response = objectMapper.readValue(responseBody, ApiResponse.class);
        assertEquals("success", response.getStatus());
        assertTrue(response.getMessage().contains("ID"));
    }

    @Test
    @DisplayName("不正なJSONでエラーが発生する")
    void testProcessWithInvalidJson() {
        // Given
        String invalidJson = "{invalid json}";
        exchange.getMessage().setBody(invalidJson);

        // When & Then
        assertThrows(Exception.class, () -> processor.process(exchange));
    }

    @Test
    @DisplayName("空のリクエストボディでエラーが発生する")
    void testProcessWithEmptyBody() {
        // Given
        exchange.getMessage().setBody("");

        // When & Then
        assertThrows(Exception.class, () -> processor.process(exchange));
    }
}

