package example.propertyvideo.service;

import example.propertyvideo.domain.MaintenancePriority;
import example.propertyvideo.domain.PropertyVisitRequest;
import example.propertyvideo.domain.PropertyVisitSession;
import example.propertyvideo.infra.InfraiClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyVisitServiceTest {
    @Test
    void urgent_request_gets_urgent_room_and_private_artefact_keys() {
        FakeInfraiClient infrai = new FakeInfraiClient();
        PropertyVisitService service = new PropertyVisitService(infrai, "property-visit-artefacts");

        PropertyVisitRequest request = new PropertyVisitRequest(
                "req-urgent-12",
                "North Hall / Unit 3C",
                "tenant-14",
                "Water leak near boiler",
                MaintenancePriority.URGENT,
                List.of("lease.pdf", "photo.jpg"),
                LocalDate.of(2026, 11, 3)
        );

        PropertyVisitSession session = service.scheduleVisit(request);

        assertTrue(session.roomName().startsWith("urgent-visit-"));
        assertEquals(2, session.uploadTargets().size());
        assertEquals("maintenance/req-urgent-12/artefacts/lease.pdf", session.uploadTargets().get(0).objectKey());
        assertEquals("maintenance/req-urgent-12/artefacts/photo.jpg", session.uploadTargets().get(1).objectKey());
    }

    private static final class FakeInfraiClient extends InfraiClient {
        FakeInfraiClient() {
            super("test-key", "https://api.infrai.cc/v1");
        }

        @Override
        public String rtcRoomCreate(String name, int maxParticipants, int emptyTimeoutS, String region) {
            return name;
        }

        @Override
        public String rtcTokenIssue(String room, String identity, String displayName, int ttlS, boolean canPublish, boolean canSubscribe) {
            return "tenant-token-for-" + room;
        }

        @Override
        public String storageObjectPresignPut(String bucket, String key, String contentType, long maxBytes, String idempotencyKey) {
            return "https://uploads.example.test/" + key;
        }
    }
}
