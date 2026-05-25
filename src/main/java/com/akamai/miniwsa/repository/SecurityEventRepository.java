package com.akamai.miniwsa.repository;

import com.akamai.miniwsa.domain.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    long countByClientIpAndReceivedAtGreaterThanEqual(String clientIp, Instant windowStart);
}
