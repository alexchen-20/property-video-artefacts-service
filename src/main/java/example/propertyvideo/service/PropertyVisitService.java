package example.propertyvideo.service;

import example.propertyvideo.domain.ArtefactUploadTarget;
import example.propertyvideo.domain.MaintenancePriority;
import example.propertyvideo.domain.PropertyVisitRequest;
import example.propertyvideo.domain.PropertyVisitSession;
import example.propertyvideo.infra.InfraiClient;

import java.util.ArrayList;
import java.util.List;

public final class PropertyVisitService {
    private final InfraiClient infrai;
    private final String bucket;

    public PropertyVisitService(InfraiClient infrai, String bucket) {
        this.infrai = infrai;
        this.bucket = bucket;
    }

    public PropertyVisitSession scheduleVisit(PropertyVisitRequest request) {
        String roomName = buildRoomName(request);
        infrai.rtcRoomCreate(roomName, 6, 600, "auto");
        String joinToken = infrai.rtcTokenIssue(
                roomName,
                request.tenantId(),
                request.propertyLabel() + " tenant",
                3600,
                true,
                true
        );

        List<ArtefactUploadTarget> uploadTargets = new ArrayList<>();
        for (String documentName : request.documentNames()) {
            String objectKey = buildObjectKey(request.requestId(), documentName);
            String uploadUrl = infrai.storageObjectPresignPut(
                    bucket,
                    objectKey,
                    contentTypeFor(documentName),
                    20_000_000L,
                    request.requestId() + ":" + documentName
            );
            uploadTargets.add(new ArtefactUploadTarget(documentName, objectKey, uploadUrl));
        }

        return new PropertyVisitSession(roomName, joinToken, request.inspectionReminderDate(), uploadTargets);
    }

    String buildRoomName(PropertyVisitRequest request) {
        String prefix = request.priority() == MaintenancePriority.URGENT ? "urgent-visit-" : "scheduled-visit-";
        return prefix + sanitize(request.requestId()) + "-" + sanitize(request.tenantId());
    }

    String buildObjectKey(String requestId, String originalFileName) {
        return "maintenance/" + sanitize(requestId) + "/artefacts/" + sanitizeFileName(originalFileName);
    }

    private static String contentTypeFor(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        return "application/octet-stream";
    }

    private static String sanitize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String sanitizeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
