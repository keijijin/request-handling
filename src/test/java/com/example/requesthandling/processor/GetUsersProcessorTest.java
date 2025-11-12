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
 * GetUsersProcessor の統合テスト
 */
@SpringBootTest
@DisplayName("ユーザー一覧取得プロセッサーのテスト")
class GetUsersProcessorTest {

    @Autowired
    private GetUsersProcessor processor;

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
    @DisplayName("ユーザー一覧を正常に取得できる")
    void testProcessSuccess() throws Exception {
        // When
        processor.process(exchange);

        // Then
        String responseBody = exchange.getMessage().getBody(String.class);
        String contentType = exchange.getMessage().getHeader(Exchange.CONTENT_TYPE, String.class);

        assertNotNull(responseBody);
        assertEquals(MediaType.APPLICATION_JSON_VALUE, contentType);

        ApiResponse response = objectMapper.readValue(responseBody, ApiResponse.class);
        assertEquals("success", response.getStatus());
        assertEquals("ユーザー一覧を取得しました", response.getMessage());
        assertNotNull(response.getData());
    }
}

