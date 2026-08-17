package com.example.distributeddb;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

final class ConsistentHashRing {
    private final List<Peer> peers;
    private final int virtualNodes;
    private final NavigableMap<Long, Peer> ring = new TreeMap<>();

    ConsistentHashRing(List<Peer> peers, int virtualNodes) {
        this.peers = peers.stream()
                .sorted(Comparator.comparing(Peer::nodeId))
                .toList();
        this.virtualNodes = virtualNodes;
        for (Peer peer : this.peers) {
            for (int replica = 0; replica < virtualNodes; replica++) {
                long slot = hash(peer.nodeId() + "#" + replica);
                while (ring.containsKey(slot)) {
                    slot++;
                }
                ring.put(slot, peer);
            }
        }
    }

    List<Peer> replicasFor(String key, int replicationFactor) {
        if (ring.isEmpty()) {
            return List.of();
        }
        int wanted = Math.min(replicationFactor, peers.size());
        LinkedHashMap<String, Peer> replicas = new LinkedHashMap<>();
        long keyHash = hash(key);
        addReplicas(ring.tailMap(keyHash, true), replicas, wanted);
        if (replicas.size() < wanted) {
            addReplicas(ring.headMap(keyHash, false), replicas, wanted);
        }
        return new ArrayList<>(replicas.values());
    }

    boolean isReplica(String key, int replicationFactor, String nodeId) {
        return replicasFor(key, replicationFactor).stream().anyMatch(peer -> peer.nodeId().equals(nodeId));
    }

    String summary() {
        StringBuilder builder = new StringBuilder("nodes=");
        for (int i = 0; i < peers.size(); i++) {
            Peer peer = peers.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append(peer.nodeId()).append("@").append(peer.endpoint());
        }
        builder.append(" virtualNodes=").append(virtualNodes);
        return builder.toString();
    }

    private void addReplicas(NavigableMap<Long, Peer> candidates, Map<String, Peer> replicas, int wanted) {
        for (Peer peer : candidates.values()) {
            replicas.putIfAbsent(peer.nodeId(), peer);
            if (replicas.size() == wanted) {
                return;
            }
        }
    }

    private static long hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(bytes).getLong() & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }
}
