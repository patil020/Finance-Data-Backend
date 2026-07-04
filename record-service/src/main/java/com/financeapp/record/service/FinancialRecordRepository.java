package com.financeapp.record.service;

import com.financeapp.record.entity.FinancialRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

/**
 * DESIGN PATTERN: Repository Pattern
 * Data access abstraction layer
 */
@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, String> {
    Page<FinancialRecord> findByUserIdAndDeletedFalse(String userId, Pageable pageable);

    Optional<FinancialRecord> findByIdAndUserIdAndDeletedFalse(String id, String userId);

    @Query("""
            select r from FinancialRecord r
            where r.userId = :userId
              and r.deleted = false
              and (:category is null or lower(r.category) = lower(:category))
              and (:type is null or r.type = :type)
              and (:fromDate is null or r.date >= :fromDate)
              and (:toDate is null or r.date <= :toDate)
            """)
    Page<FinancialRecord> filterRecords(
            @Param("userId") String userId,
            @Param("category") String category,
            @Param("type") FinancialRecord.RecordType type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
