package com.example.distributeddb;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class ClusterNode implements AutoCloseable {
    private final NodeConfig config;
    private final KeyValueStore store;
    private final ConsistentHashRing ring;
    private final PeerClient peerClient;
    private final TcpServer server;
    private final ScheduledExecutorService heartbeatExecutor;
    private final AtomicLong versionClock;
    private volatile Set<String> liveNodeIds;
    private volatile String leaderNodeId;

    public ClusterNode(NodeConfig config) {
        this.config = config;
        Path logFile = config.dataDir().resolve(config.nodeId() + ".wal");
        this.store = new KeyValueStore(logFile);
        this.ring = new ConsistentHashRing(config.peers(), config.virtualNodes());
        this.peerClient = new PeerClient(config.connectTimeoutMillis());
        this.server = new TcpServer(config.nodeId(), config.bindHost(), config.port(), new CommandHandler(this)::handle);
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "heartbeat-" + config.nodeId());
            thread.setDaemon(true);
            return thread;
        });
        this.versionClock = new AtomicLong(Math.max(Instant.now().toEpochMilli(), store.maxVersion()));
        this.liveNodeIds = Set.of(config.nodeId());
        this.leaderNodeId = config.nodeId();
    }

    public void start() {
        server.start();
        heartbeatExecutor.scheduleAtFixedRate(this::refreshLivePeers, 0, 750, TimeUnit.MILLISECONDS);
        heartbeatExecutor.schedule(this::recoverFromPeers, 1200, TimeUnit.MILLISECONDS);
    }

    String put(String key, String value) {
        if (!isLeader()) {
            return forwardToLeader("COORD_PUT " + Codec.encode(key) + " " + Codec.encode(value));
        }
        return coordinatePut(key, value);
    }

    String delete(String key) {
        if (!isLeader()) {
            return forwardToLeader("COORD_DELETE " + Codec.encode(key));
        }
        return coordinateDelete(key);
    }

    String coordinatePut(String key, String value) {
        if (!isLeader()) {
            return forwardToLeader("COORD_PUT " + Codec.encode(key) + " " + Codec.encode(value));
        }
        StoredValue record = new StoredValue(key, value, nextVersion(), false);
        return replicate(record, config.writeQuorum(), "PUT");
    }

    String coordinateDelete(String key) {
        if (!isLeader()) {
            return forwardToLeader("COORD_DELETE " + Codec.encode(key));
        }
        StoredValue record = new StoredValue(key, null, nextVersion(), true);
        return replicate(record, config.writeQuorum(), "DELETE");
    }

    String get(String key) {
        List<Peer> replicas = ring.replicasFor(key, config.replicationFactor());
        List<ReplicaRead> reads = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Peer replica : replicas) {
            if (replica.nodeId().equals(config.nodeId())) {
                StoredValue local = store.get(key).orElseGet(() -> StoredValue.missing(key));
                reads.add(new ReplicaRead(replica, local));
                continue;
            }
            String response = peerClient.send(replica, "INTERNAL_GET " + Codec.encode(key));
            if (response.startsWith("OK ")) {
                reads.add(new ReplicaRead(replica, parseInternalValue(key, response)));
            } else {
                errors.add(replica.nodeId() + "=" + response);
            }
        }
        if (reads.size() < config.readQuorum()) {
            return "ERROR read quorum failed key=" + key
                    + " required=" + config.readQuorum()
                    + " successful=" + reads.size()
                    + " errors=" + String.join(",", errors);
        }
        StoredValue newest = reads.stream()
                .map(ReplicaRead::value)
                .max(Comparator.comparingLong(StoredValue::version))
                .orElseGet(() -> StoredValue.missing(key));
        readRepair(newest, reads);
        if (newest.version() == 0 || newest.deleted()) {
            return "NOT_FOUND key=" + key;
        }
        return "VALUE key=" + key + " version=" + newest.version() + " " + newest.value();
    }

    String applyInternalPut(String key, long version, String value) {
        boolean applied = store.apply(new StoredValue(key, value, version, false));
        bumpClock(version);
        return "OK applied=" + applied + " node=" + config.nodeId();
    }

    String applyInternalDelete(String key, long version) {
        boolean applied = store.apply(new StoredValue(key, null, version, true));
        bumpClock(version);
        return "OK applied=" + applied + " node=" + config.nodeId();
    }

    String internalGet(String key) {
        StoredValue value = store.get(key).orElseGet(() -> StoredValue.missing(key));
        return "OK " + value.version() + " " + value.deleted() + " " + Codec.encode(value.value() == null ? "" : value.value());
    }

    String internalSync(String targetNodeId) {
        List<String> lines = store.allRecords().stream()
                .filter(record -> ring.isReplica(record.key(), config.replicationFactor(), targetNodeId))
                .sorted(Comparator.comparing(StoredValue::key))
                .map(StoredValue::toSyncLine)
                .toList();
        if (lines.isEmpty()) {
            return "OK 0 -";
        }
        return "OK " + lines.size() + " " + Codec.encode(String.join("\n", lines));
    }

    int recoverFromPeers() {
        int recovered = 0;
        for (Peer peer : config.peers()) {
            if (peer.nodeId().equals(config.nodeId())) {
                continue;
            }
            String response = peerClient.send(peer, "INTERNAL_SYNC " + config.nodeId());
            if (!response.startsWith("OK ")) {
                continue;
            }
            recovered += applySyncResponse(response);
        }
        return recovered;
    }

    String status() {
        return "OK node=" + config.nodeId()
                + " role=" + (isLeader() ? "LEADER" : "FOLLOWER")
                + " leader=" + leaderNodeId
                + " live=" + String.join(",", liveNodeIds)
                + " rf=" + config.replicationFactor()
                + " readQuorum=" + config.readQuorum()
                + " writeQuorum=" + config.writeQuorum()
                + " records=" + store.allRecords().size();
    }

    String ringSummary() {
        return "OK " + ring.summary();
    }

    String ping() {
        return "OK node=" + config.nodeId() + " leader=" + leaderNodeId;
    }

    String help() {
        return "OK commands=PUT <key> <value>; GET <key>; DELETE <key>; STATUS; RING; RECOVER";
    }

    String leaderNodeId() {
        return leaderNodeId;
    }

    Set<String> liveNodeIds() {
        return liveNodeIds;
    }

    private String replicate(StoredValue record, int requiredAcks, String operation) {
        List<Peer> replicas = ring.replicasFor(record.key(), config.replicationFactor());
        int acknowledgements = 0;
        List<String> errors = new ArrayList<>();
        for (Peer replica : replicas) {
            String response;
            if (replica.nodeId().equals(config.nodeId())) {
                store.apply(record);
                response = "OK self";
            } else if (record.deleted()) {
                response = peerClient.send(replica, "INTERNAL_DELETE " + Codec.encode(record.key()) + " " + record.version());
            } else {
                response = peerClient.send(replica, "INTERNAL_PUT " + Codec.encode(record.key()) + " " + record.version() + " " + Codec.encode(record.value()));
            }
            if (response.startsWith("OK")) {
                acknowledgements++;
            } else {
                errors.add(replica.nodeId() + "=" + response);
            }
        }
        if (acknowledgements < requiredAcks) {
            return "ERROR write quorum failed operation=" + operation
                    + " key=" + record.key()
                    + " version=" + record.version()
                    + " required=" + requiredAcks
                    + " acknowledged=" + acknowledgements
                    + " replicas=" + peerIds(replicas)
                    + " errors=" + String.join(",", errors);
        }
        return "OK operation=" + operation
                + " key=" + record.key()
                + " version=" + record.version()
                + " acknowledged=" + acknowledgements
                + " quorum=" + requiredAcks
                + " replicas=" + peerIds(replicas);
    }

    private String forwardToLeader(String encodedCommand) {
        String leaderId = leaderNodeId;
        if (leaderId == null || leaderId.equals(config.nodeId())) {
            return "ERROR no remote leader available";
        }
        return config.peer(leaderId)
                .map(peer -> peerClient.send(peer, encodedCommand))
                .orElse("ERROR leader " + leaderId + " is not in peer list");
    }

    private void readRepair(StoredValue newest, List<ReplicaRead> reads) {
        if (newest.version() == 0) {
            return;
        }
        for (ReplicaRead read : reads) {
            if (read.value().version() >= newest.version()) {
                continue;
            }
            if (read.peer().nodeId().equals(config.nodeId())) {
                store.apply(newest);
            } else if (newest.deleted()) {
                peerClient.send(read.peer(), "INTERNAL_DELETE " + Codec.encode(newest.key()) + " " + newest.version());
            } else {
                peerClient.send(read.peer(), "INTERNAL_PUT " + Codec.encode(newest.key()) + " " + newest.version() + " " + Codec.encode(newest.value()));
            }
        }
    }

    private int applySyncResponse(String response) {
        String[] fields = response.split(" ", 3);
        if (fields.length != 3 || fields[2].equals("-")) {
            return 0;
        }
        String payload = Codec.decode(fields[2]);
        int applied = 0;
        for (String line : payload.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            StoredValue record = StoredValue.fromSyncLine(line);
            if (ring.isReplica(record.key(), config.replicationFactor(), config.nodeId()) && store.apply(record)) {
                applied++;
            }
            bumpClock(record.version());
        }
        return applied;
    }

    private StoredValue parseInternalValue(String key, String response) {
        String[] fields = response.split(" ", 4);
        if (fields.length != 4) {
            throw new IllegalArgumentException("invalid internal get response: " + response);
        }
        long version = Long.parseLong(fields[1]);
        boolean deleted = Boolean.parseBoolean(fields[2]);
        String value = Codec.decode(fields[3]);
        return new StoredValue(key, deleted ? null : value, version, deleted);
    }

    private void refreshLivePeers() {
        LinkedHashSet<String> live = new LinkedHashSet<>();
        live.add(config.nodeId());
        for (Peer peer : config.peers()) {
            if (peer.nodeId().equals(config.nodeId())) {
                continue;
            }
            String response = peerClient.send(peer, "PING");
            if (response.startsWith("OK")) {
                live.add(peer.nodeId());
            }
        }
        this.liveNodeIds = Collections.unmodifiableSet(live);
        this.leaderNodeId = electLeader(live);
    }

    private String electLeader(Set<String> live) {
        for (Peer peer : config.peers()) {
            if (live.contains(peer.nodeId())) {
                return peer.nodeId();
            }
        }
        return config.nodeId();
    }

    private boolean isLeader() {
        return config.nodeId().equals(leaderNodeId);
    }

    private long nextVersion() {
        return versionClock.updateAndGet(previous -> Math.max(previous + 1, Instant.now().toEpochMilli()));
    }

    private void bumpClock(long observedVersion) {
        versionClock.updateAndGet(previous -> Math.max(previous, observedVersion));
    }

    private String peerIds(List<Peer> peers) {
        return peers.stream().map(Peer::nodeId).collect(Collectors.joining(","));
    }

    @Override
    public void close() {
        heartbeatExecutor.shutdownNow();
        server.close();
    }

    private record ReplicaRead(Peer peer, StoredValue value) {
    }
}
