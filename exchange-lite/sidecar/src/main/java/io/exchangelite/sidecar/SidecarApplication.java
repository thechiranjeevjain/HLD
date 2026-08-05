package io.exchangelite.sidecar;

import java.util.concurrent.CountDownLatch;

public final class SidecarApplication {
    private SidecarApplication() {
    }

    public static void main(String[] args) throws Exception {
        SidecarConfig config = SidecarConfig.fromEnvironment();
        SidecarHttpServer server = new SidecarHttpServer(config, new EngineIpcGateway(config));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "exchange-sidecar-shutdown"));
        server.start();
        System.out.println("ExchangeLite sidecar httpPort=" + config.httpPort()
                + " engineIpc=" + config.engineIpcHost() + ":" + config.engineIpcPort());
        new CountDownLatch(1).await();
    }
}
