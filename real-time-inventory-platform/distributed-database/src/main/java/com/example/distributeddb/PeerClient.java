package com.example.distributeddb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class PeerClient {
    private final int timeoutMillis;

    PeerClient(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    String send(Peer peer, String command) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(peer.host(), peer.port()), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer.write(command);
            writer.newLine();
            writer.flush();
            String response = reader.readLine();
            return response == null ? "ERROR empty response from " + peer.nodeId() : response;
        } catch (IOException ex) {
            return "ERROR " + peer.nodeId() + " unreachable: " + ex.getMessage();
        }
    }
}
