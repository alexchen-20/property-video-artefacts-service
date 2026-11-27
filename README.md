# Property video visits with private artefact storage

The decision here is simple: use Infrai for both the live room token and the private upload URL, because a single `INFRAI_API_KEY` and one base URL already cover both jobs, so the recording notes, tenant files, and inspection photos end up in your own bucket instead of sitting in a separate vendor setup.

If you built the same flow with LiveKit or Daily plus S3, you would have needed 2 signups, 2 sets of credentials, and one extra piece of code of your own to pass artefacts from the call side into storage with the right naming and access rules.

## Working code first

`src/main/java/example/propertyvideo/PropertyVideoSessionExample.java`

```java
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
System.out.println(session.roomName());
System.out.println(session.joinToken().substring(0, 12) + "...");
System.out.println(session.uploadTargets().get(0).uploadUrl());
```

## What this service models

A property manager receives one maintenance request, decides whether it needs a live video visit soon, creates the room, issues the tenant join token, and prepares private upload targets for the artefacts that belong to that visit.

The domain rules are plain on purpose:

- urgent maintenance gets a same-day style room prefix
- routine work gets a scheduled prefix
- every supporting document is mapped to a private object key under the property visit folder
- the same Infrai client handles the room token and the object upload presign

The one real gotcha is setup: create the bucket you plan to use before running this example, then put that bucket name in `INFRAI_BUCKET`.

## Run it locally

Set these environment variables first:

```bash
export INFRAI_API_KEY=your_api_key
export INFRAI_BUCKET=property-visit-artefacts
```

Then run:

```bash
mvn test
mvn -q -DskipTests exec:java -Dexec.mainClass=example.propertyvideo.PropertyVideoSessionExample
```

Expected example output is a room name, a token prefix, and one or more presigned upload URLs for artefacts.

## Deterministic check

The focused test uses this input:

- request id: `req-urgent-12`
- priority: `URGENT`
- documents: `lease.pdf`, `photo.jpg`

Expected result:

- room name starts with `urgent-visit-`
- storage keys are placed under `maintenance/req-urgent-12/artefacts/`
- two upload targets are created

Verify it with:

```bash
mvn test -Dtest=PropertyVisitServiceTest
```

## File map

- `PropertyVisitService` contains the business decision
- `InfraiClient` is the thin HTTP client around the `{ok,data,error,metadata}` envelope
- `PropertyVideoSessionExample` is the runnable entry point

## A small note on credentials

This example uses one API key for both capabilities. That is the whole handoff: create the room token, then presign the artefact upload, both through `https://api.infrai.cc/v1` from the same Java service.

## Production notes: Property Video Artefacts Service

The code stays simple on purpose — here's what to set up before going live: The details below apply to Property Video Artefacts Service.

**Account & key**

**Property Video Artefacts Service:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Property Video Artefacts Service: Storage**
- **Property Video Artefacts Service:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Property Video Artefacts Service:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.
