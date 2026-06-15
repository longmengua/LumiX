/*
 * 檔案用途：Java 原始碼檔案，屬於 java21-match-hub 交易服務。
 */
package com.example.exchange.interfaces.web;

import com.example.exchange.infra.config.ApiAuthProperties;
import com.example.exchange.infra.config.SecurityControlsProperties;
import com.example.exchange.interfaces.web.interceptor.ApiAuthenticationInterceptor;
import com.example.exchange.interfaces.web.interceptor.ProtectedApiSecurityInterceptor;
import com.example.exchange.interfaces.web.interceptor.RequestLoggingInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 組態 (Configuration)
 * --------------------------
 * - 此類別用於 Spring MVC 的組態設定
 * - 可以在這裡註冊攔截器 (Interceptor)、跨域設定 (CORS)、訊息轉換器 (MessageConverter)、格式化器 (Formatter) 等
 */
@Configuration // 告訴 Spring 這是一個配置類，會在啟動時載入
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiAuthProperties apiAuthProperties;

    private final SecurityControlsProperties securityControlsProperties;

    private final ObjectMapper objectMapper;

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

        registry.addInterceptor(new ApiAuthenticationInterceptor(apiAuthProperties, objectMapper))
                .addPathPatterns(securityControlsProperties.getProtectedPathPatterns());
    }

    /**
     * 針對前端交易頁面靜態資源取消長快取。
     * 這可避免瀏覽器吃到舊版 exchange.js/exchange.css，導致你看到的版面不一致。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path localStaticDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "static");

        registry.addResourceHandler(
                        "/exchange.html",
                        "/exchange.js",
                        "/exchange.css",
                        "/exchange-dev.html"
                )
                .addResourceLocations(
                        localStaticDir.toUri().toString(),
                        "classpath:/static/"
                )
                .setCacheControl(CacheControl.noStore().mustRevalidate())
                .setCachePeriod(0)
                .setUseLastModified(false)
                .resourceChain(false);
    }

    /**
     * 將根路徑直接導向新版交易頁面。
     * 過去若環境仍有舊的 index.html（例如 target/classes/static）時，會誤導到不是這一版的頁面。
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/exchange.html");
    }
}
