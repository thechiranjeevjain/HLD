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
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UnixDomainSocketIpcServer implements EngineIpcServer {
    private final Path socketPath;
    private final RuntimeCommandRegistry registry;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocketChannel channel;

    public UnixDomainSocketIpcServer(Path socketPath, RuntimeCommandRegistry registry) {
        this.socketPath = socketPath;
        this.registry = registry;
    }

    @Override
    public void start() throws IOException {
        Files.deleteIfExists(socketPath);
        channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        channel.bind(UnixDomainSocketAddress.of(socketPath));
        running.set(true);
        Thread acceptThread = new Thread(this::acceptLoop, "exchange-ipc-uds-acceptor");
        acceptThread.setDaemon(false);
        acceptThread.start();
    }

    @Override
    public void close() throws IOException {
        running.set(false);
        if (channel != null) {
            channel.close();
        }
        workers.shutdownNow();
        Files.deleteIfExists(socketPath);
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                SocketChannel client = channel.accept();
                workers.execute(() -> handle(client));
            } catch (IOException ex) {
                if (running.get()) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void handle(SocketChannel client) {
        try (client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(client), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Channels.newOutputStream(client), StandardCharsets.UTF_8))) {
            RuntimeCommand command = RuntimeCommandCodec.decodeCommand(reader.readLine());
            RuntimeResponse response = registry.handle(command);
            writer.write(RuntimeCommandCodec.encodeResponse(response));
            writer.flush();
        } catch (RuntimeException | IOException ex) {
            try {
                client.close();
            } catch (IOException ignored) {
                // Connection is already unusable.
            }
        }
    }
}
