package com.financeapp.dashboard.client;

import com.financeapp.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * DESIGN PATTERN: Feign Client Pattern (Service-to-Service Communication)
 * Makes synchronous REST calls to Record Service
 */
@FeignClient(name = "record-service", path = "/api")
public interface RecordServiceClient {

    @GetMapping("/records")
    ApiResponse<RecordPageResponse> getRecords(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    );
}
