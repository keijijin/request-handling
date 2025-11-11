package com.example.requesthandling.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * エラーメッセージ設定プロパティ
 */
@Component
@ConfigurationProperties(prefix = "error")
@Data
public class ErrorMessageProperties {
    
    private Map<String, String> messages = new HashMap<>();
    
    /**
     * ステータスコードに対応するエラーメッセージを取得
     * 
     * @param statusCode HTTPステータスコード
     * @return エラーメッセージ
     */
    public String getMessage(int statusCode) {
        return messages.getOrDefault(
            String.valueOf(statusCode), 
            messages.getOrDefault("default", "エラーが発生しました")
        );
    }
}

