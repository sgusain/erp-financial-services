package com.erp.transaction.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return template -> {
            var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                if (authHeader != null) {
                    template.header("Authorization", authHeader);
                }
                String correlationId = attributes.getRequest().getHeader(CORRELATION_ID_HEADER);
                if (correlationId != null) {
                    template.header(CORRELATION_ID_HEADER, correlationId);
                }
            }
        };
    }
}
