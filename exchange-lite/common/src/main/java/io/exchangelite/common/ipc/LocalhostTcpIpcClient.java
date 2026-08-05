package io.exchangelite.common.ipc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class LocalhostTcpIpcClient implements EngineIpcClient {
    private final String host;
    private final int port;
    private final Duration timeout;

    public LocalhostTcpIpcClient(String host, int port, Duration timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public RuntimeResponse execute(RuntimeCommand command) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.toIntExact(timeout.toMillis()));
            socket.setSoTimeout(Math.toIntExact(timeout.toMillis()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer.write(RuntimeCommandCodec.encodeCommand(command));
            writer.flush();
            String responseLine = reader.readLine();
            return RuntimeCommandCodec.decodeResponse(responseLine);
        }
    }
}
