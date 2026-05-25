package com.akamai.miniwsa.repository;

import com.akamai.miniwsa.domain.SecurityEvent;
import com.akamai.miniwsa.stats.dto.ActionRow;
import com.akamai.miniwsa.stats.dto.AttackerRow;
import com.akamai.miniwsa.stats.dto.CategoryRow;
import com.akamai.miniwsa.stats.dto.PathRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    long countByClientIpAndReceivedAtGreaterThanEqual(String clientIp, Instant windowStart);

    // cast(:from as instant) / cast(:to as instant) gives Hibernate a typed binding so
    // PostgreSQL receives CAST(? AS timestamptz) IS NULL instead of untyped ? IS NULL,
    // which PostgreSQL cannot resolve when the value is null (error 42P18).
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE " +
           "(:configId IS NULL OR e.configId = :configId) AND " +
           "(cast(:from as instant) IS NULL OR e.receivedAt >= :from) AND " +
           "(cast(:to as instant) IS NULL OR e.receivedAt <= :to)")
    long countFiltered(@Param("configId") Long configId,
                       @Param("from") Instant from,
                       @Param("to") Instant to);

    @Query("SELECT new com.akamai.miniwsa.stats.dto.CategoryRow(e.rule.category, COUNT(e), AVG(e.threatScore)) " +
           "FROM SecurityEvent e WHERE " +
           "(:configId IS NULL OR e.configId = :configId) AND " +
           "(cast(:from as instant) IS NULL OR e.receivedAt >= :from) AND " +
           "(cast(:to as instant) IS NULL OR e.receivedAt <= :to) " +
           "GROUP BY e.rule.category")
    List<CategoryRow> countByCategory(@Param("configId") Long configId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    @Query("SELECT new com.akamai.miniwsa.stats.dto.ActionRow(e.action, COUNT(e)) " +
           "FROM SecurityEvent e WHERE " +
           "(:configId IS NULL OR e.configId = :configId) AND " +
           "(cast(:from as instant) IS NULL OR e.receivedAt >= :from) AND " +
           "(cast(:to as instant) IS NULL OR e.receivedAt <= :to) " +
           "GROUP BY e.action")
    List<ActionRow> countByAction(@Param("configId") Long configId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    @Query("SELECT new com.akamai.miniwsa.stats.dto.AttackerRow(e.clientIp, COUNT(e), AVG(e.threatScore)) " +
           "FROM SecurityEvent e WHERE " +
           "(:configId IS NULL OR e.configId = :configId) AND " +
           "(cast(:from as instant) IS NULL OR e.receivedAt >= :from) AND " +
           "(cast(:to as instant) IS NULL OR e.receivedAt <= :to) " +
           "GROUP BY e.clientIp ORDER BY COUNT(e) DESC")
    List<AttackerRow> topAttackers(@Param("configId") Long configId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to,
                                   Pageable pageable);

    @Query("SELECT new com.akamai.miniwsa.stats.dto.PathRow(e.path, COUNT(e)) " +
           "FROM SecurityEvent e WHERE " +
           "(:configId IS NULL OR e.configId = :configId) AND " +
           "(cast(:from as instant) IS NULL OR e.receivedAt >= :from) AND " +
           "(cast(:to as instant) IS NULL OR e.receivedAt <= :to) " +
           "GROUP BY e.path ORDER BY COUNT(e) DESC")
    List<PathRow> topTargetedPaths(@Param("configId") Long configId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to,
                                   Pageable pageable);
}
