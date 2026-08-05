package io.exchangelite.common.domain;

public enum OrderSide {
    BUY((byte) 1),
    SELL((byte) 2);

    private final byte code;

    OrderSide(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static OrderSide fromCode(byte code) {
        for (OrderSide side : values()) {
            if (side.code == code) {
                return side;
            }
        }
        throw new IllegalArgumentException("Unknown order side code: " + code);
    }
}
