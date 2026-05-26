package com.akamai.miniwsa.config;

import com.akamai.miniwsa.ingestion.IngestionLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final IngestionLoggingInterceptor ingestionLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ingestionLoggingInterceptor)
                .addPathPatterns("/v1/events/ingest");
    }
}
