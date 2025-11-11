package com.example.requesthandling.controller;

import com.example.requesthandling.config.ErrorMessageProperties;
import com.example.requesthandling.model.ErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * カスタムエラーコントローラー
 * 404, 405などのHTTPエラーをハンドリング
 */
@RestController
public class CustomErrorController implements ErrorController {

    private final ErrorMessageProperties errorMessageProperties;

    public CustomErrorController(ErrorMessageProperties errorMessageProperties) {
        this.errorMessageProperties = errorMessageProperties;
    }

    /**
     * エラーハンドリング
     */
    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String method = request.getMethod();
        
        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;
        String path = requestUri != null ? requestUri.toString() : "不明";
        
        // カスタムエラーメッセージを取得
        String customMessage = errorMessageProperties.getMessage(statusCode);
        
        // エラーレスポンスを構築
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(statusCode)
                .message(customMessage)
                .details(getDetailMessage(statusCode, path, method))
                .timestamp(LocalDateTime.now().toString())
                .path(path)
                .method(method)
                .build();
        
        HttpStatus httpStatus = HttpStatus.resolve(statusCode);
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
    
    /**
     * ステータスコードに応じた詳細メッセージを生成
     */
    private String getDetailMessage(int statusCode, String path, String method) {
        return switch (statusCode) {
            case 404 -> String.format("パス '%s' は存在しません。URLを確認してください。", path);
            case 405 -> String.format("パス '%s' に対して、メソッド '%s' は許可されていません。", path, method);
            case 500 -> "サーバー内部でエラーが発生しました。管理者に連絡してください。";
            default -> String.format("ステータスコード %d のエラーが発生しました。", statusCode);
        };
    }
}

