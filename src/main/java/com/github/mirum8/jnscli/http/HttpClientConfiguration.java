package com.github.mirum8.jnscli.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;

@Configuration
public class HttpClientConfiguration {
    @Bean
    public HttpClient httpClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(java.time.Duration.ofSeconds(60))
            .build();
    }
}
