package io.exchangelite.engine.runtime;

import java.util.Collection;

public final class EngineJson {
    private EngineJson() {
    }

    public static String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        builder.append('"');
        return builder.toString();
    }

    public static String array(Collection<String> jsonValues) {
        return "[" + String.join(",", jsonValues) + "]";
    }
}
