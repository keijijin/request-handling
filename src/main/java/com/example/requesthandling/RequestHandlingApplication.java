package com.example.requesthandling;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.component.servlet.springboot.ServletMappingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Request Handling Application
 * Camel for Spring Boot with Undertow
 */
@SpringBootApplication(exclude = {ServletMappingAutoConfiguration.class})
public class RequestHandlingApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequestHandlingApplication.class, args);
    }

    /**
     * CamelサーブレットをSpringコンテナに登録
     * ServletMappingAutoConfigurationを無効化し、手動で登録します
     */
    @Bean
    public ServletRegistrationBean<CamelHttpTransportServlet> servletRegistrationBean() {
        ServletRegistrationBean<CamelHttpTransportServlet> registration = 
            new ServletRegistrationBean<>(new CamelHttpTransportServlet(), "/api/*");
        registration.setName("CamelServlet");
        return registration;
    }
}

