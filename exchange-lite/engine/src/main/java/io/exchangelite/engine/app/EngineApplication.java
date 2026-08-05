package io.exchangelite.engine.app;

import io.exchangelite.engine.network.BinaryTcpServer;
import io.exchangelite.engine.network.EngineIpcServer;
import io.exchangelite.engine.network.LocalhostTcpIpcServer;
import io.exchangelite.engine.runtime.EngineConfig;
import io.exchangelite.engine.runtime.RuntimeCommandRegistry;
import io.exchangelite.engine.runtime.TradingEngineRuntime;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class EngineApplication {
    private EngineApplication() {
    }

    public static void main(String[] args) throws Exception {
        EngineConfig config = EngineConfig.fromEnvironment();
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        TradingEngineRuntime runtime = new TradingEngineRuntime(config);
        AtomicReference<BinaryTcpServer> dataPlane = new AtomicReference<>();
        AtomicReference<EngineIpcServer> ipcServer = new AtomicReference<>();

        RuntimeCommandRegistry registry = new RuntimeCommandRegistry(runtime, shutdownLatch::countDown);
        dataPlane.set(new BinaryTcpServer(config.dataPlanePort(), runtime));
        ipcServer.set(new LocalhostTcpIpcServer(config.ipcPort(), registry));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            closeQuietly(dataPlane.get());
            closeQuietly(ipcServer.get());
        }, "exchange-engine-shutdown"));

        dataPlane.get().start();
        ipcServer.get().start();
        System.out.println("ExchangeLite engine dataPort=" + config.dataPlanePort() + " ipcPort=" + config.ipcPort());
        shutdownLatch.await();
        closeQuietly(dataPlane.get());
        closeQuietly(ipcServer.get());
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // Process is shutting down.
        }
    }
}
