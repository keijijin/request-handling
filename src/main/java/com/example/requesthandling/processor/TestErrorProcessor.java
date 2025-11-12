package com.example.requesthandling.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

/**
 * テストエラープロセッサー
 * 意図的にエラーを発生させて500エラーのテストに使用
 */
@Component
public class TestErrorProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        throw new RuntimeException("これはテスト用のエラーです");
    }
}

