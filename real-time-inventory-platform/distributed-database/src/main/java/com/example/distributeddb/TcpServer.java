package com.example.distributeddb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

final class TcpServer implements AutoCloseable {
    private final String bindHost;
    private final int port;
    private final Function<String, String> commandHandler;
    private final ExecutorService clients;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread acceptThread;

    TcpServer(String nodeId, String bindHost, int port, Function<String, String> commandHandler) {
        this.bindHost = bindHost;
        this.port = port;
        this.commandHandler = Objects.requireNonNull(commandHandler, "commandHandler");
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, "tcp-client-" + nodeId);
            thread.setDaemon(true);
            return thread;
        };
        this.clients = Executors.newCachedThreadPool(threadFactory);
    }

    void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(bindHost, port));
        } catch (IOException ex) {
            running.set(false);
            throw new IllegalStateException("Could not bind TCP server to " + bindHost + ":" + port, ex);
        }
        acceptThread = new Thread(this::acceptLoop, "tcp-accept-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                clients.submit(() -> handleClient(socket));
            } catch (IOException ex) {
                if (running.get()) {
                    System.err.println("Accept failed on port " + port + ": " + ex.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            String response = line == null ? "ERROR empty command" : commandHandler.apply(line);
            writer.write(response);
            writer.newLine();
            writer.flush();
        } catch (RuntimeException ex) {
            try {
                socket.getOutputStream().write(("ERROR " + ex.getMessage() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // The client has already gone away.
            }
        } catch (IOException ignored) {
            // A disconnected client is not a node failure.
        }
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // Closing the listener is enough to stop the accept loop.
        }
        clients.shutdownNow();
    }
}
