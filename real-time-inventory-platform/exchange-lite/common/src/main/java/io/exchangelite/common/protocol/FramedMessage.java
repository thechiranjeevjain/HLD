package io.exchangelite.common.protocol;

import java.util.Arrays;
import java.util.Objects;

public record FramedMessage(long correlationId, BinaryMessageType type, byte[] payload) {
    public FramedMessage {
        type = Objects.requireNonNull(type, "type");
        payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }
}
