package com.example.requesthandling.processor;

import com.example.requesthandling.model.ApiResponse;
import com.example.requesthandling.model.User;
import com.example.requesthandling.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * ユーザー作成プロセッサー
 */
@Component("createUserProcessor")
public class CreateUserProcessor implements Processor {

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        // JSONボディを手動でUserオブジェクトに変換
        String jsonBody = exchange.getIn().getBody(String.class);
        User inputUser = objectMapper.readValue(jsonBody, User.class);
        User createdUser = userService.createUser(inputUser);
        
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.CREATED.value());
        ApiResponse response = ApiResponse.builder()
                .status("success")
                .message("ユーザーを作成しました (ID: " + createdUser.getId() + ")")
                .data(createdUser)
                .build();
        String jsonResponse = objectMapper.writeValueAsString(response);
        exchange.getMessage().setBody(jsonResponse);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}

