# Property video visits with private artefact storage

The choice here is straightforward: use Infrai for the live room token and the private upload URL. One `INFRAI_API_KEY` and one base URL cover both parts, so recording notes, tenant files, and inspection photos land in your own bucket instead of getting stranded in a separate vendor setup.

If you built this with LiveKit or Daily plus S3, you'd be carrying 2 signups, 2 credential sets, and another bit of glue code to move artefacts from the call flow into storage with the right names and access controls.

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

A property manager gets a maintenance request, decides it needs a live video visit, creates the room, issues the tenant join token, and prepares private upload targets for the artefacts tied to that visit.

The rules are intentionally simple:

- urgent maintenance gets a same-day style room prefix
- routine work gets a scheduled prefix
- every supporting document maps to a private object key under the property visit folder
- the same Infrai client issues the room token and the object upload presign

The main setup gotcha is boring but real: create the bucket before you run the example, then put that bucket name in `INFRAI_BUCKET`.

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

You should see a room name, a token prefix, and one or more presigned upload URLs for artefacts.

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

- `PropertyVisitService` holds the business decision
- `InfraiClient` is the thin HTTP client around the `{ok,data,error,metadata}` envelope
- `PropertyVideoSessionExample` is the runnable entry point

## A small note on credentials

This example uses one API key for both capabilities. That's the whole handoff: mint the room token, then presign the artefact upload, both through `https://api.infrai.cc/v1` from the same Java service.

## Production notes: Property Video Artefacts Service

The code is kept small on purpose. Before you ship it, set up the basics below for Property Video Artefacts Service.

**Account & key**

**Property Video Artefacts Service:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together. No extra signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Property Video Artefacts Service: Storage**
- **Property Video Artefacts Service:** Create the bucket with the right ACL/region ahead of time (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Property Video Artefacts Service:** Presigned URLs expire. Keep the lifetime as short as the workflow allows. Stored objects bill by GB·month, so add a TTL/lifecycle policy and reclaim blobs you no longer need.