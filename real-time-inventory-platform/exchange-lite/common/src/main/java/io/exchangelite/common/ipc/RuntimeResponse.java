package io.exchangelite.common.ipc;

public record RuntimeResponse(boolean ok, int statusCode, String body) {
    public RuntimeResponse {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be an HTTP-compatible status");
        }
        body = body == null ? "" : body;
    }

    public static RuntimeResponse ok(String body) {
        return new RuntimeResponse(true, 200, body);
    }

    public static RuntimeResponse accepted(String body) {
        return new RuntimeResponse(true, 202, body);
    }

    public static RuntimeResponse error(int statusCode, String body) {
        return new RuntimeResponse(false, statusCode, body);
    }
}
