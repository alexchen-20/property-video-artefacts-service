package example.propertyvideo.infra;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfraiClient {
    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;

    public InfraiClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String rtcRoomCreate(String name, int maxParticipants, int emptyTimeoutS, String region) {
        String body = "{" +
                quote("name") + ":" + quote(name) + "," +
                quote("max_participants") + ":" + maxParticipants + "," +
                quote("empty_timeout_s") + ":" + emptyTimeoutS + "," +
                quote("region") + ":" + quote(region) +
                "}";
        String json = sendWithRetry("POST", "/rtc/room/create", body);
        return extractStringField(json, "name");
    }

    public String rtcTokenIssue(String room, String identity, String displayName, int ttlS, boolean canPublish, boolean canSubscribe) {
        String body = "{" +
                quote("room") + ":" + quote(room) + "," +
                quote("identity") + ":" + quote(identity) + "," +
                quote("display_name") + ":" + quote(displayName) + "," +
                quote("ttl_s") + ":" + ttlS + "," +
                quote("can_publish") + ":" + canPublish + "," +
                quote("can_subscribe") + ":" + canSubscribe +
                "}";
        String json = sendWithRetry("POST", "/rtc/token/issue", body);
        return firstStringValue(json, "token", "jwt", "value");
    }

    public String storageObjectPresignPut(String bucket, String key, String contentType, long maxBytes, String idempotencyKey) {
        String path = "/storage/object/presign/" + encodeSegment(bucket) + "/" + encodeSegment(key);
        String body = "{" +
                quote("op") + ":" + quote("put") + "," +
                quote("expires_seconds") + ":900," +
                quote("content_type") + ":" + quote(contentType) + "," +
                quote("max_bytes") + ":" + maxBytes + "," +
                quote("idempotency_key") + ":" + quote(idempotencyKey) +
                "}";
        String json = sendWithRetry("POST", path, body);
        return firstStringValue(json, "url", "upload_url");
    }

    private String sendWithRetry(String method, String path, String body) {
        int attempts = 0;
        long sleepMs = 300L;
        while (true) {
            attempts++;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(20))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String responseBody = response.body();
                if (responseBody != null && responseBody.contains("\"ok\":false")) {
                    String code = firstStringValue(responseBody, "code");
                    String message = firstStringValue(responseBody, "message");
                    throw new InfraiError(code == null ? "INFRAI_ERROR" : code, message, response.statusCode());
                }
                if (response.statusCode() == 429 && attempts < 4) {
                    String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
                    sleepQuietly(retryAfter == null ? sleepMs : Long.parseLong(retryAfter) * 1000L);
                    sleepMs *= 2;
                    continue;
                }
                if (response.statusCode() >= 500) {
                    throw new IllegalStateException("Unexpected server response: " + response.statusCode());
                }
                return extractDataJson(responseBody);
            } catch (InfraiError e) {
                throw e;
            } catch (IOException | InterruptedException e) {
                if (attempts >= 4) {
                    throw new IllegalStateException("Request failed", e);
                }
                sleepQuietly(sleepMs);
                sleepMs *= 2;
            }
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    private static String extractDataJson(String json) {
        int dataIndex = json.indexOf("\"data\"");
        if (dataIndex < 0) {
            return json;
        }
        int colon = json.indexOf(':', dataIndex);
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length()) {
            return "{}";
        }
        char open = json.charAt(start);
        if (open == '{') {
            int depth = 0;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(start, i + 1);
                    }
                }
            }
        }
        return json.substring(start).trim();
    }

    private static String extractStringField(String json, String field) {
        String value = firstStringValue(json, field);
        if (value == null) {
            throw new IllegalStateException("Missing field in response: " + field);
        }
        return value;
    }

    private static String firstStringValue(String json, String... fields) {
        for (String field : fields) {
            Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String encodeSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
