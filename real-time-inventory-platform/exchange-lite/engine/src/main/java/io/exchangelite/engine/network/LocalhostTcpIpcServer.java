package io.exchangelite.engine.network;

import io.exchangelite.common.ipc.RuntimeCommand;
import io.exchangelite.common.ipc.RuntimeCommandCodec;
import io.exchangelite.common.ipc.RuntimeResponse;
import io.exchangelite.engine.runtime.RuntimeCommandRegistry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalhostTcpIpcServer implements EngineIpcServer {
    private final int port;
    private final RuntimeCommandRegistry registry;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public LocalhostTcpIpcServer(int port, RuntimeCommandRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    @Override
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running.set(true);
        Thread acceptThread = new Thread(this::acceptLoop, "exchange-ipc-tcp-acceptor");
        acceptThread.setDaemon(false);
        acceptThread.start();
    }

    public int port() {
        if (serverSocket == null) {
            throw new IllegalStateException("server not started");
        }
        return serverSocket.getLocalPort();
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
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            RuntimeCommand command = RuntimeCommandCodec.decodeCommand(line);
            RuntimeResponse response = registry.handle(command);
            writer.write(RuntimeCommandCodec.encodeResponse(response));
            writer.flush();
        } catch (RuntimeException | IOException ex) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Connection is already unusable.
            }
        }
    }
}
