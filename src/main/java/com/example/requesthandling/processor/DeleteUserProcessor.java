package com.example.requesthandling.processor;

import com.example.requesthandling.model.ApiResponse;
import com.example.requesthandling.model.ErrorResponse;
import com.example.requesthandling.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ユーザー削除プロセッサー
 */
@Component("deleteUserProcessor")
public class DeleteUserProcessor implements Processor {

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        String userId = exchange.getIn().getHeader("id", String.class);
        boolean deleted = userService.deleteUser(userId);
        
        if (deleted) {
            ApiResponse response = ApiResponse.builder()
                    .status("success")
                    .message("ユーザーを削除しました")
                    .data("Deleted User ID: " + userId)
                    .build();
            String jsonResponse = objectMapper.writeValueAsString(response);
            exchange.getMessage().setBody(jsonResponse);
            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        } else {
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code(404)
                    .message("指定されたリソースが見つかりません")
                    .details("ID '" + userId + "' のユーザーは存在しません")
                    .timestamp(LocalDateTime.now().toString())
                    .path("/api/users/" + userId)
                    .method("DELETE")
                    .build();
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            exchange.getMessage().setBody(jsonResponse);
            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
    }
}

