package com.example.requesthandling.processor;

import com.example.requesthandling.model.ApiResponse;
import com.example.requesthandling.model.ErrorResponse;
import com.example.requesthandling.model.User;
import com.example.requesthandling.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ユーザー更新プロセッサー
 */
@Component("updateUserProcessor")
public class UpdateUserProcessor implements Processor {

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        String userId = exchange.getIn().getHeader("id", String.class);
        // JSONボディを手動でUserオブジェクトに変換
        String jsonBody = exchange.getIn().getBody(String.class);
        User inputUser = objectMapper.readValue(jsonBody, User.class);
        Optional<User> updatedUser = userService.updateUser(userId, inputUser);
        
        if (updatedUser.isPresent()) {
            ApiResponse response = ApiResponse.builder()
                    .status("success")
                    .message("ユーザーを更新しました")
                    .data(updatedUser.get())
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
                    .method("PUT")
                    .build();
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            exchange.getMessage().setBody(jsonResponse);
            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
    }
}

