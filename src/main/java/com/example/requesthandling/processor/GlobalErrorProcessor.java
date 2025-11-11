package com.example.requesthandling.processor;

import com.example.requesthandling.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * グローバルエラー処理プロセッサー
 */
@Component("globalErrorProcessor")
public class GlobalErrorProcessor implements Processor {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(500)
                .message("内部サーバーエラーが発生しました")
                .details(exception != null ? exception.getMessage() : "不明なエラー")
                .timestamp(LocalDateTime.now().toString())
                .path(exchange.getIn().getHeader(Exchange.HTTP_URI, String.class))
                .method(exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class))
                .build();
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        exchange.getMessage().setBody(jsonResponse);
    }
}

