package io.exchangelite.engine.network;

import java.io.IOException;

public interface EngineIpcServer extends AutoCloseable {
    void start() throws IOException;

    @Override
    void close() throws IOException;
}
