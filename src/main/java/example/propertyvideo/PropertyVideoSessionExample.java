package example.propertyvideo;

import example.propertyvideo.config.ServiceConfig;
import example.propertyvideo.domain.MaintenancePriority;
import example.propertyvideo.domain.PropertyVisitRequest;
import example.propertyvideo.domain.PropertyVisitSession;
import example.propertyvideo.infra.InfraiClient;
import example.propertyvideo.service.PropertyVisitService;

import java.time.LocalDate;
import java.util.List;

public final class PropertyVideoSessionExample {
    public static void main(String[] args) {
        ServiceConfig config = ServiceConfig.fromEnv();
        InfraiClient infrai = new InfraiClient(config.apiKey(), config.baseUrl());
        PropertyVisitService service = new PropertyVisitService(infrai, config.bucket());

        PropertyVisitRequest request = new PropertyVisitRequest(
                "req-401",
                "Sunrise Apartments / Unit 4B",
                "tenant-77",
                "Leaking tap under kitchen sink",
                MaintenancePriority.URGENT,
                List.of("lease-renewal.pdf", "inspection-checklist.pdf"),
                LocalDate.of(2026, 10, 2)
        );

        PropertyVisitSession session = service.scheduleVisit(request);

        System.out.println("Room: " + session.roomName());
        System.out.println("Token prefix: " + session.joinToken().substring(0, Math.min(12, session.joinToken().length())) + "...");
        session.uploadTargets().forEach(target ->
                System.out.println("Upload " + target.originalFileName() + " -> " + target.uploadUrl()));
    }
}
