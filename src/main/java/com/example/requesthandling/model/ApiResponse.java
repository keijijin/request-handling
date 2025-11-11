package com.example.requesthandling.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 正常なAPIレスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    
    /**
     * ステータス
     */
    private String status;
    
    /**
     * メッセージ
     */
    private String message;
    
    /**
     * データ
     */
    private Object data;
}

