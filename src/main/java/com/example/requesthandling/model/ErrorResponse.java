package com.example.requesthandling.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * エラーレスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * エラーコード（HTTPステータスコード）
     */
    private int code;
    
    /**
     * エラーメッセージ
     */
    private String message;
    
    /**
     * 詳細情報
     */
    private String details;
    
    /**
     * エラー発生日時（ISO 8601形式の文字列）
     */
    private String timestamp;
    
    /**
     * リクエストパス
     */
    private String path;
    
    /**
     * リクエストメソッド
     */
    private String method;
}

