package com.example.requesthandling.processor;

import com.example.requesthandling.model.ApiResponse;
import com.example.requesthandling.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * ユーザー一覧取得プロセッサー
 */
@Component("getUsersProcessor")
public class GetUsersProcessor implements Processor {

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        ApiResponse response = ApiResponse.builder()
                .status("success")
                .message("ユーザー一覧を取得しました")
                .data(userService.getAllUsers())
                .build();
        
        String jsonResponse = objectMapper.writeValueAsString(response);
        exchange.getMessage().setBody(jsonResponse);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}

