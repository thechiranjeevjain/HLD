package io.exchangelite.common.protocol;

public enum BinaryMessageType {
    NEW_ORDER((byte) 1),
    CANCEL_ORDER((byte) 2),
    EXECUTION_REPORT((byte) 3),
    REJECT((byte) 4),
    HEARTBEAT((byte) 5);

    private final byte code;

    BinaryMessageType(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static BinaryMessageType fromCode(byte code) {
        for (BinaryMessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new ProtocolException("Unknown binary message type: " + code);
    }
}
