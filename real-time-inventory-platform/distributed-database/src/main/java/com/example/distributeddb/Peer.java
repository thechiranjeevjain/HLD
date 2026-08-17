package com.example.distributeddb;

import java.util.Objects;

public record Peer(String nodeId, String host, int port) {
    public Peer {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(host, "host");
        if (nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public static Peer parse(String spec) {
        String[] idAndAddress = spec.split("=", 2);
        if (idAndAddress.length != 2) {
            throw new IllegalArgumentException("Peer must use nodeId=host:port format: " + spec);
        }
        int portSeparator = idAndAddress[1].lastIndexOf(':');
        if (portSeparator < 1 || portSeparator == idAndAddress[1].length() - 1) {
            throw new IllegalArgumentException("Peer address must use host:port format: " + spec);
        }
        String host = idAndAddress[1].substring(0, portSeparator);
        int port = Integer.parseInt(idAndAddress[1].substring(portSeparator + 1));
        return new Peer(idAndAddress[0], host, port);
    }

    public String endpoint() {
        return host + ":" + port;
    }
}
