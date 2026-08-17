package io.exchangelite.common.ipc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RuntimeCommandCodec {
    private RuntimeCommandCodec() {
    }

    public static String encodeCommand(RuntimeCommand command) {
        StringBuilder builder = new StringBuilder(command.type().name());
        for (Map.Entry<String, String> entry : command.arguments().entrySet()) {
            builder.append(' ')
                    .append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(entry.getValue()));
        }
        return builder.append('\n').toString();
    }

    public static RuntimeCommand decodeCommand(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("command line is empty");
        }
        String[] parts = line.trim().split(" ");
        RuntimeCommandType type = RuntimeCommandType.valueOf(parts[0].toUpperCase());
        Map<String, String> args = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            int separator = parts[i].indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("invalid command argument: " + parts[i]);
            }
            args.put(decode(parts[i].substring(0, separator)), decode(parts[i].substring(separator + 1)));
        }
        return new RuntimeCommand(type, args);
    }

    public static String encodeResponse(RuntimeResponse response) {
        String body = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(response.body().getBytes(StandardCharsets.UTF_8));
        return (response.ok() ? "OK" : "ERR") + " " + response.statusCode() + " " + body + "\n";
    }

    public static RuntimeResponse decodeResponse(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("response line is empty");
        }
        String[] parts = line.trim().split(" ", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("response line must have status, code, body");
        }
        boolean ok = switch (parts[0]) {
            case "OK" -> true;
            case "ERR" -> false;
            default -> throw new IllegalArgumentException("unknown response status: " + parts[0]);
        };
        int statusCode = Integer.parseInt(parts[1]);
        String body = new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);
        return new RuntimeResponse(ok, statusCode, body);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
