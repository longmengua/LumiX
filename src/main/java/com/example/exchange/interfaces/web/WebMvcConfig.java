/*
 * 檔案用途：Java 原始碼檔案，屬於 java21-match-hub 交易服務。
 */
package com.example.exchange.interfaces.web;

import com.example.exchange.infra.config.SecurityControlsProperties;
import com.example.exchange.interfaces.web.interceptor.ProtectedApiSecurityInterceptor;
import com.example.exchange.interfaces.web.interceptor.RequestLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 組態 (Configuration)
 * --------------------------
 * - 此類別用於 Spring MVC 的組態設定
 * - 可以在這裡註冊攔截器 (Interceptor)、跨域設定 (CORS)、訊息轉換器 (MessageConverter)、格式化器 (Formatter) 等
 */
@Configuration // 告訴 Spring 這是一個配置類，會在啟動時載入
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityControlsProperties securityControlsProperties;

    /**
     * 註冊自定義攔截器
     * - 在這裡我們註冊 RequestLoggingInterceptor
     * - 可以攔截所有 HTTP 請求，並做統一處理 (例如：請求日誌、權限驗證、參數校驗等)
     *
     * @param registry 攔截器註冊器，用來新增/配置攔截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor()) // 新增一個 RequestLoggingInterceptor
                .addPathPatterns("/**"); // 設定攔截路徑，"**" 表示攔截所有請求

        registry.addInterceptor(new ProtectedApiSecurityInterceptor(securityControlsProperties))
                .addPathPatterns(securityControlsProperties.getProtectedPathPatterns());
    }
}
