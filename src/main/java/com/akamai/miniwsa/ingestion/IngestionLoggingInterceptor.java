package com.akamai.miniwsa.ingestion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IngestionLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IngestionLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("Ingest request received");
        return true;
    }
}
