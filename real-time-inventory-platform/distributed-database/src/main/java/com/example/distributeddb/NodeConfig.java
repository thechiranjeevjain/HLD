package com.example.distributeddb;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record NodeConfig(
        String nodeId,
        String bindHost,
        int port,
        List<Peer> peers,
        Path dataDir,
        int replicationFactor,
        int readQuorum,
        int writeQuorum,
        int virtualNodes,
        int connectTimeoutMillis
) {
    public NodeConfig {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(bindHost, "bindHost");
        Objects.requireNonNull(peers, "peers");
        Objects.requireNonNull(dataDir, "dataDir");
        if (nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        if (bindHost.isBlank()) {
            throw new IllegalArgumentException("bindHost must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (peers.isEmpty()) {
            throw new IllegalArgumentException("at least one peer is required");
        }
        Map<String, Peer> unique = new LinkedHashMap<>();
        for (Peer peer : peers) {
            if (unique.put(peer.nodeId(), peer) != null) {
                throw new IllegalArgumentException("duplicate peer id: " + peer.nodeId());
            }
        }
        if (!unique.containsKey(nodeId)) {
            throw new IllegalArgumentException("peers must include this node id: " + nodeId);
        }
        if (replicationFactor < 1 || replicationFactor > unique.size()) {
            throw new IllegalArgumentException("replicationFactor must be between 1 and peer count");
        }
        if (readQuorum < 1 || readQuorum > replicationFactor) {
            throw new IllegalArgumentException("readQuorum must be between 1 and replicationFactor");
        }
        if (writeQuorum < 1 || writeQuorum > replicationFactor) {
            throw new IllegalArgumentException("writeQuorum must be between 1 and replicationFactor");
        }
        if (virtualNodes < 1) {
            throw new IllegalArgumentException("virtualNodes must be positive");
        }
        if (connectTimeoutMillis < 100) {
            throw new IllegalArgumentException("connectTimeoutMillis must be at least 100");
        }
        peers = List.copyOf(peers);
    }

    public static NodeConfig fromArgs(String[] args) {
        Map<String, String> values = parseArgs(args);
        String nodeId = required(values, "node-id");
        int port = Integer.parseInt(required(values, "port"));
        List<Peer> peers = parsePeers(required(values, "peers"));
        int replicationFactor = Integer.parseInt(values.getOrDefault("replication-factor", Integer.toString(Math.min(3, peers.size()))));
        int majority = (replicationFactor / 2) + 1;
        return new NodeConfig(
                nodeId,
                values.getOrDefault("bind-host", "127.0.0.1"),
                port,
                peers,
                Path.of(values.getOrDefault("data-dir", "data/" + nodeId)),
                replicationFactor,
                Integer.parseInt(values.getOrDefault("read-quorum", Integer.toString(majority))),
                Integer.parseInt(values.getOrDefault("write-quorum", Integer.toString(majority))),
                Integer.parseInt(values.getOrDefault("virtual-nodes", "64")),
                Integer.parseInt(values.getOrDefault("connect-timeout-ms", "750"))
        );
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Expected --name value argument, got: " + arg);
            }
            String name = arg.substring(2);
            if (name.isBlank()) {
                throw new IllegalArgumentException("Argument name must not be blank");
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for --" + name);
            }
            parsed.put(name, args[++i]);
        }
        return parsed;
    }

    private static String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required --" + name);
        }
        return value;
    }

    private static List<Peer> parsePeers(String rawPeers) {
        List<Peer> peers = new ArrayList<>();
        for (String rawPeer : rawPeers.split(",")) {
            if (!rawPeer.isBlank()) {
                peers.add(Peer.parse(rawPeer.trim()));
            }
        }
        return peers;
    }

    public Peer self() {
        return peer(nodeId).orElseThrow();
    }

    public Optional<Peer> peer(String id) {
        return peers.stream().filter(peer -> peer.nodeId().equals(id)).findFirst();
    }
}
