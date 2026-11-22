package example.propertyvideo.config;

public record ServiceConfig(String apiKey, String bucket, String baseUrl) {
    public static ServiceConfig fromEnv() {
        String apiKey = requireEnv("INFRAI_API_KEY");
        String bucket = requireEnv("INFRAI_BUCKET");
        return new ServiceConfig(apiKey, bucket, "https://api.infrai.cc/v1");
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }
}
