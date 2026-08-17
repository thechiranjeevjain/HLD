package io.exchangelite.common.domain;

public enum OrderType {
    LIMIT((byte) 1),
    MARKET((byte) 2);

    private final byte code;

    OrderType(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static OrderType fromCode(byte code) {
        for (OrderType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown order type code: " + code);
    }
}
