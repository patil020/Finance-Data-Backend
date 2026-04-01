package com.financeapp.dao.specification;

import com.financeapp.entities.FinancialRecord;
import com.financeapp.enums.RecordType;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class FinancialRecordSpecification {

    private FinancialRecordSpecification() {
    }

    public static Specification<FinancialRecord> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<FinancialRecord> hasType(RecordType type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<FinancialRecord> hasCategory(String category) {
        return (root, query, cb) -> category == null || category.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("category")), "%" + category.toLowerCase() + "%");
    }

    public static Specification<FinancialRecord> onOrAfter(LocalDate startDate) {
        return (root, query, cb) -> startDate == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("recordDate"), startDate);
    }

    public static Specification<FinancialRecord> onOrBefore(LocalDate endDate) {
        return (root, query, cb) -> endDate == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("recordDate"), endDate);
    }
}
