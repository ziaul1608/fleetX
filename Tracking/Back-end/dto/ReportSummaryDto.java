package com.fleetx.tracking.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReportSummaryDto(
        String reportType,
        LocalDate startDate,
        LocalDate endDate,
        String statusFilter,
        int totalRecords,
        List<String> columns,
        List<Map<String, Object>> rows,
        Instant generatedAt
) {
}
