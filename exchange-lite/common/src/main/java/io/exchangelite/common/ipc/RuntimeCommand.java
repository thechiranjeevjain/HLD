package io.exchangelite.common.ipc;

import java.util.Map;
import java.util.Objects;

public record RuntimeCommand(RuntimeCommandType type, Map<String, String> arguments) {
    public RuntimeCommand {
        type = Objects.requireNonNull(type, "type");
        arguments = Map.copyOf(arguments == null ? Map.of() : arguments);
    }

    public static RuntimeCommand of(RuntimeCommandType type) {
        return new RuntimeCommand(type, Map.of());
    }
}
