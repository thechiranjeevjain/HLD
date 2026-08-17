package io.exchangelite.common.protocol;

import io.exchangelite.common.domain.CancelRequest;
import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.OrderSide;
import io.exchangelite.common.domain.OrderType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class BinaryProtocol {
    public static final int MAGIC = 0x584C5445;
    public static final byte VERSION = 1;
    public static final int HEADER_BYTES = Integer.BYTES + 1 + 1 + Integer.BYTES + Long.BYTES;
    public static final int MAX_PAYLOAD_BYTES = 1024 * 1024;

    private BinaryProtocol() {
    }

    public static byte[] encodeFrame(FramedMessage message) {
        byte[] payload = message.payload();
        if (payload.length > MAX_PAYLOAD_BYTES) {
            throw new ProtocolException("Payload too large: " + payload.length);
        }
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES + payload.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(MAGIC);
        buffer.put(VERSION);
        buffer.put(message.type().code());
        buffer.putInt(payload.length);
        buffer.putLong(message.correlationId());
        buffer.put(payload);
        return buffer.array();
    }

    public static FramedMessage decodeFrame(byte[] frame) {
        if (frame.length < HEADER_BYTES) {
            throw new ProtocolException("Frame shorter than header");
        }
        ByteBuffer buffer = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new ProtocolException("Invalid magic");
        }
        byte version = buffer.get();
        if (version != VERSION) {
            throw new ProtocolException("Unsupported protocol version: " + version);
        }
        BinaryMessageType type = BinaryMessageType.fromCode(buffer.get());
        int payloadLength = buffer.getInt();
        if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES) {
            throw new ProtocolException("Invalid payload length: " + payloadLength);
        }
        if (frame.length != HEADER_BYTES + payloadLength) {
            throw new ProtocolException("Frame length does not match header length");
        }
        long correlationId = buffer.getLong();
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);
        return new FramedMessage(correlationId, type, payload);
    }

    public static byte[] encodeOrderRequest(OrderRequest request) {
        int size = stringSize(request.market())
                + stringSize(request.clientOrderId())
                + stringSize(request.accountId())
                + 1
                + 1
                + Long.BYTES
                + Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
        putString(buffer, request.market());
        putString(buffer, request.clientOrderId());
        putString(buffer, request.accountId());
        buffer.put(request.side().code());
        buffer.put(request.type().code());
        buffer.putLong(request.priceTicks());
        buffer.putInt(request.quantity());
        return buffer.array();
    }

    public static OrderRequest decodeOrderRequest(byte[] payload) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
            String market = getString(buffer);
            String clientOrderId = getString(buffer);
            String accountId = getString(buffer);
            OrderSide side = OrderSide.fromCode(buffer.get());
            OrderType type = OrderType.fromCode(buffer.get());
            long priceTicks = buffer.getLong();
            int quantity = buffer.getInt();
            if (buffer.hasRemaining()) {
                throw new ProtocolException("Unexpected trailing bytes in order request");
            }
            return new OrderRequest(market, clientOrderId, accountId, side, type, priceTicks, quantity);
        } catch (RuntimeException ex) {
            if (ex instanceof ProtocolException protocolException) {
                throw protocolException;
            }
            throw new ProtocolException("Cannot decode order request", ex);
        }
    }

    public static byte[] encodeCancelRequest(CancelRequest request) {
        int size = stringSize(request.market()) + stringSize(request.clientOrderId()) + stringSize(request.accountId());
        ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
        putString(buffer, request.market());
        putString(buffer, request.clientOrderId());
        putString(buffer, request.accountId());
        return buffer.array();
    }

    public static CancelRequest decodeCancelRequest(byte[] payload) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
            CancelRequest request = new CancelRequest(getString(buffer), getString(buffer), getString(buffer));
            if (buffer.hasRemaining()) {
                throw new ProtocolException("Unexpected trailing bytes in cancel request");
            }
            return request;
        } catch (RuntimeException ex) {
            if (ex instanceof ProtocolException protocolException) {
                throw protocolException;
            }
            throw new ProtocolException("Cannot decode cancel request", ex);
        }
    }

    public static byte[] encodeText(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public static String decodeText(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    private static int stringSize(String value) {
        return Integer.BYTES + value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static void putString(ByteBuffer buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    private static String getString(ByteBuffer buffer) {
        if (buffer.remaining() < Integer.BYTES) {
            throw new ProtocolException("String length missing");
        }
        int length = buffer.getInt();
        if (length < 0 || length > buffer.remaining()) {
            throw new ProtocolException("Invalid string length: " + length);
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
