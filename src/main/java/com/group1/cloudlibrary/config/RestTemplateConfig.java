package com.group1.cloudlibrary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置：用于调用大模型 / Embedding 第三方 API
 * <p>超时固定为连接 10 秒、读取 60 秒，避免大模型生成慢时 Read timed out。</p>
 */
@Configuration
public class RestTemplateConfig {

    /** 连接超时：10 秒 */
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    /** 读取超时：60 秒，给大模型足够时间生成回答 */
    private static final int READ_TIMEOUT_MS = 60_000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }
}
