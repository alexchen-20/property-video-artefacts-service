package example.propertyvideo.domain;

import java.time.LocalDate;
import java.util.List;

public record PropertyVisitRequest(
        String requestId,
        String propertyLabel,
        String tenantId,
        String issueSummary,
        MaintenancePriority priority,
        List<String> documentNames,
        LocalDate inspectionReminderDate
) {
}
