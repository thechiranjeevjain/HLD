package io.exchangelite.sidecar;

import io.exchangelite.common.ipc.EngineIpcClient;
import io.exchangelite.common.ipc.LocalhostTcpIpcClient;
import io.exchangelite.common.ipc.RuntimeCommand;
import io.exchangelite.common.ipc.RuntimeResponse;

import java.io.IOException;

public final class EngineIpcGateway implements IpcGateway {
    private final EngineIpcClient client;

    public EngineIpcGateway(SidecarConfig config) {
        this.client = new LocalhostTcpIpcClient(config.engineIpcHost(), config.engineIpcPort(), config.engineTimeout());
    }

    @Override
    public RuntimeResponse execute(RuntimeCommand command) throws IOException {
        return client.execute(command);
    }
}
