package com.financeapp.dao;

import com.financeapp.entities.FinancialRecord;
import com.financeapp.enums.RecordType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialRecordDao extends JpaRepository<FinancialRecord, Long>, JpaSpecificationExecutor<FinancialRecord> {

    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    Page<FinancialRecord> findAllByDeletedFalse(Pageable pageable);

    Page<FinancialRecord> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("select coalesce(sum(fr.amount), 0) from FinancialRecord fr where fr.deleted = false and fr.type = :type")
    BigDecimal sumByType(@Param("type") RecordType type);

    @Query("""
            select fr.category, coalesce(sum(fr.amount), 0)
            from FinancialRecord fr
            where fr.deleted = false
            group by fr.category
            order by coalesce(sum(fr.amount), 0) desc
            """)
    List<Object[]> fetchCategoryWiseTotals();

    @Query("""
            select year(fr.recordDate), month(fr.recordDate),
                   coalesce(sum(case when fr.type = com.financeapp.enums.RecordType.INCOME then fr.amount else 0 end), 0),
                   coalesce(sum(case when fr.type = com.financeapp.enums.RecordType.EXPENSE then fr.amount else 0 end), 0)
            from FinancialRecord fr
            where fr.deleted = false
            group by year(fr.recordDate), month(fr.recordDate)
            order by year(fr.recordDate), month(fr.recordDate)
            """)
    List<Object[]> fetchMonthlyTrends();
}
