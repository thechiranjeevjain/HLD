package io.exchangelite.common.protocol;

import io.exchangelite.common.domain.CancelRequest;
import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.OrderSide;
import io.exchangelite.common.domain.OrderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BinaryProtocolTest {
    @Test
    void encodesAndDecodesOrderRequest() {
        OrderRequest request = new OrderRequest("btc-usd", "C-1", "acct-1", OrderSide.BUY, OrderType.LIMIT, 42_000_00, 10);

        OrderRequest decoded = BinaryProtocol.decodeOrderRequest(BinaryProtocol.encodeOrderRequest(request));

        assertEquals("BTC-USD", decoded.market());
        assertEquals(request.clientOrderId(), decoded.clientOrderId());
        assertEquals(request.accountId(), decoded.accountId());
        assertEquals(request.side(), decoded.side());
        assertEquals(request.type(), decoded.type());
        assertEquals(request.priceTicks(), decoded.priceTicks());
        assertEquals(request.quantity(), decoded.quantity());
    }

    @Test
    void encodesAndDecodesCancelRequest() {
        CancelRequest request = new CancelRequest("eth-usd", "C-9", "acct-9");

        CancelRequest decoded = BinaryProtocol.decodeCancelRequest(BinaryProtocol.encodeCancelRequest(request));

        assertEquals("ETH-USD", decoded.market());
        assertEquals("C-9", decoded.clientOrderId());
        assertEquals("acct-9", decoded.accountId());
    }

    @Test
    void framesRoundTripPayloadWithoutExposingMutablePayload() {
        byte[] payload = new byte[]{1, 2, 3};
        FramedMessage frame = new FramedMessage(77L, BinaryMessageType.HEARTBEAT, payload);
        payload[0] = 9;

        FramedMessage decoded = BinaryProtocol.decodeFrame(BinaryProtocol.encodeFrame(frame));

        assertEquals(77L, decoded.correlationId());
        assertEquals(BinaryMessageType.HEARTBEAT, decoded.type());
        assertArrayEquals(new byte[]{1, 2, 3}, decoded.payload());
    }

    @Test
    void rejectsInvalidFrameMagic() {
        byte[] encoded = BinaryProtocol.encodeFrame(new FramedMessage(1L, BinaryMessageType.HEARTBEAT, new byte[0]));
        encoded[0] = 0;

        assertThrows(ProtocolException.class, () -> BinaryProtocol.decodeFrame(encoded));
    }
}
