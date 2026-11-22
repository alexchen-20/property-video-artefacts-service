package example.propertyvideo.domain;

import java.time.LocalDate;
import java.util.List;

public record PropertyVisitSession(
        String roomName,
        String joinToken,
        LocalDate inspectionReminderDate,
        List<ArtefactUploadTarget> uploadTargets
) {
}
