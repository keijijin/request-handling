package com.example.requesthandling.processor;

import com.example.requesthandling.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ヘルスチェックプロセッサー
 */
@Component("healthCheckProcessor")
public class HealthCheckProcessor implements Processor {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        ApiResponse response = ApiResponse.builder()
                .status("UP")
                .message("アプリケーションは正常に稼働しています")
                .data(LocalDateTime.now().toString())
                .build();
        String jsonResponse = objectMapper.writeValueAsString(response);
        exchange.getMessage().setBody(jsonResponse);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}

