package example.propertyvideo.infra;

public final class InfraiError extends RuntimeException {
    private final String code;
    private final int statusCode;

    public InfraiError(String code, String message, int statusCode) {
        super(message == null ? code : message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String code() {
        return code;
    }

    public int statusCode() {
        return statusCode;
    }
}
