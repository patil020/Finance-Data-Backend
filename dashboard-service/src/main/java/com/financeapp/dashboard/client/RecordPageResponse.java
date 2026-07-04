package com.financeapp.dashboard.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RecordPageResponse {
    private List<RecordResponse> content = new ArrayList<>();
    private int number;
    private int totalPages;
    private boolean last;

    @Data
    public static class RecordResponse {
        private String id;
        private String userId;
        private BigDecimal amount;
        private String type;
        private String category;
        private boolean deleted;
    }
}
