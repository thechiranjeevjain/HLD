package io.exchangelite.common.ipc;

import java.io.IOException;

public interface EngineIpcClient {
    RuntimeResponse execute(RuntimeCommand command) throws IOException;
}
