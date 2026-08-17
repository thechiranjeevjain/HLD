package io.exchangelite.sidecar;

import io.exchangelite.common.ipc.RuntimeCommand;
import io.exchangelite.common.ipc.RuntimeResponse;

import java.io.IOException;

public interface IpcGateway {
    RuntimeResponse execute(RuntimeCommand command) throws IOException;
}
