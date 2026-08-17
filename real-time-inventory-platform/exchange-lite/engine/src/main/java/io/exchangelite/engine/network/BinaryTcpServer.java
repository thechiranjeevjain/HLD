package io.exchangelite.engine.network;

import io.exchangelite.common.domain.CancelRequest;
import io.exchangelite.common.domain.ExecutionReport;
import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.protocol.BinaryMessageType;
import io.exchangelite.common.protocol.BinaryProtocol;
import io.exchangelite.common.protocol.FramedMessage;
import io.exchangelite.common.protocol.ProtocolException;
import io.exchangelite.engine.runtime.TradingEngineRuntime;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BinaryTcpServer implements AutoCloseable {
    private final int port;
    private final TradingEngineRuntime runtime;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public BinaryTcpServer(int port, TradingEngineRuntime runtime) {
        this.port = port;
        this.runtime = runtime;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running.set(true);
        Thread acceptThread = new Thread(this::acceptLoop, "exchange-data-tcp-acceptor");
        acceptThread.setDaemon(false);
        acceptThread.start();
    }

    @Override
    public void close() throws IOException {
        running.set(false);
        if (serverSocket != null) {
            serverSocket.close();
        }
        workers.shutdownNow();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                workers.execute(() -> handle(socket));
            } catch (IOException ex) {
                if (running.get()) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void handle(Socket socket) {
        long sessionId = runtime.registerSession();
        try (socket;
             InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            while (running.get()) {
                FramedMessage frame = readFrame(input);
                FramedMessage response = handleFrame(frame);
                output.write(BinaryProtocol.encodeFrame(response));
                output.flush();
            }
        } catch (EOFException ignored) {
            // Peer closed the session.
        } catch (IOException | ProtocolException ex) {
            // A production build would log structured connection metadata here.
        } finally {
            runtime.unregisterSession(sessionId);
        }
    }

    private FramedMessage handleFrame(FramedMessage frame) {
        try {
            if (frame.type() == BinaryMessageType.NEW_ORDER) {
                OrderRequest request = BinaryProtocol.decodeOrderRequest(frame.payload());
                ExecutionReport report = runtime.submitOrder(request);
                return new FramedMessage(
                        frame.correlationId(),
                        BinaryMessageType.EXECUTION_REPORT,
                        BinaryProtocol.encodeText(runtime.executionReportJson(report))
                );
            }
            if (frame.type() == BinaryMessageType.CANCEL_ORDER) {
                CancelRequest request = BinaryProtocol.decodeCancelRequest(frame.payload());
                ExecutionReport report = runtime.cancel(request);
                return new FramedMessage(
                        frame.correlationId(),
                        BinaryMessageType.EXECUTION_REPORT,
                        BinaryProtocol.encodeText(runtime.executionReportJson(report))
                );
            }
            if (frame.type() == BinaryMessageType.HEARTBEAT) {
                return new FramedMessage(frame.correlationId(), BinaryMessageType.HEARTBEAT, BinaryProtocol.encodeText("pong"));
            }
            return reject(frame, "unsupported request type");
        } catch (RuntimeException ex) {
            return reject(frame, ex.getMessage());
        }
    }

    private FramedMessage reject(FramedMessage frame, String reason) {
        String body = "{\"error\":\"" + reason.replace("\"", "\\\"") + "\"}";
        return new FramedMessage(frame.correlationId(), BinaryMessageType.REJECT, BinaryProtocol.encodeText(body));
    }

    private FramedMessage readFrame(InputStream input) throws IOException {
        byte[] header = readExactly(input, BinaryProtocol.HEADER_BYTES);
        int payloadLength = ByteBuffer.wrap(header)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt(Integer.BYTES + 1 + 1);
        if (payloadLength < 0 || payloadLength > BinaryProtocol.MAX_PAYLOAD_BYTES) {
            throw new ProtocolException("Invalid payload length: " + payloadLength);
        }
        byte[] payload = readExactly(input, payloadLength);
        byte[] frame = Arrays.copyOf(header, BinaryProtocol.HEADER_BYTES + payloadLength);
        System.arraycopy(payload, 0, frame, BinaryProtocol.HEADER_BYTES, payloadLength);
        runtime.metrics().recordBytesRead(frame.length);
        return BinaryProtocol.decodeFrame(frame);
    }

    private byte[] readExactly(InputStream input, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(buffer, offset, length - offset);
            if (read == -1) {
                throw new EOFException();
            }
            offset += read;
        }
        return buffer;
    }
}
