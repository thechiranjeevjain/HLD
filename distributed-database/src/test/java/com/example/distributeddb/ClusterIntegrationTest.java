package com.example.distributeddb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterIntegrationTest {
    @TempDir
    Path tempDir;

    private final List<ClusterNode> nodes = new ArrayList<>();
    private List<Peer> peers;
    private final PeerClient client = new PeerClient(1_000);

    @AfterEach
    void tearDown() {
        for (ClusterNode node : nodes) {
            node.close();
        }
        nodes.clear();
    }

    @Test
    void followerForwardsWriteAndReadQuorumFindsReplicatedValue() {
        startThreeNodeCluster();
        await(() -> nodes.stream().allMatch(node -> node.liveNodeIds().size() == 3 && node.leaderNodeId().equals("node1")));

        String put = client.send(peers.get(1), "PUT user:42 Alice Smith");
        String get = client.send(peers.get(2), "GET user:42");

        assertTrue(put.startsWith("OK operation=PUT"), put);
        assertTrue(get.startsWith("VALUE key=user:42"), get);
        assertTrue(get.endsWith("Alice Smith"), get);
    }

    @Test
    void stoppedReplicaCanRecoverMissedQuorumWriteFromPeers() {
        startThreeNodeCluster();
        await(() -> nodes.stream().allMatch(node -> node.liveNodeIds().size() == 3 && node.leaderNodeId().equals("node1")));

        nodes.get(2).close();
        await(() -> nodes.get(0).liveNodeIds().size() == 2 && nodes.get(1).liveNodeIds().size() == 2);

        String putWhileNode3IsDown = client.send(peers.get(0), "PUT invoice:7 paid");
        assertTrue(putWhileNode3IsDown.startsWith("OK operation=PUT"), putWhileNode3IsDown);

        ClusterNode restartedNode3 = new ClusterNode(configFor(2));
        nodes.set(2, restartedNode3);
        restartedNode3.start();
        await(() -> nodes.stream().allMatch(node -> node.liveNodeIds().size() == 3));

        String recover = client.send(peers.get(2), "RECOVER");
        String localRead = client.send(peers.get(2), "INTERNAL_GET " + Codec.encode("invoice:7"));

        assertTrue(recover.startsWith("OK recovered="), recover);
        assertTrue(localRead.contains(Codec.encode("paid")), localRead);
    }

    @Test
    void writeFailsWhenQuorumCannotBeReached() {
        startThreeNodeCluster();
        await(() -> nodes.stream().allMatch(node -> node.liveNodeIds().size() == 3 && node.leaderNodeId().equals("node1")));

        nodes.get(1).close();
        nodes.get(2).close();
        await(() -> nodes.get(0).liveNodeIds().size() == 1);

        String response = client.send(peers.get(0), "PUT isolated value");

        assertTrue(response.startsWith("ERROR write quorum failed"), response);
    }

    private void startThreeNodeCluster() {
        int p1 = freePort();
        int p2 = freePort();
        int p3 = freePort();
        peers = List.of(
                new Peer("node1", "127.0.0.1", p1),
                new Peer("node2", "127.0.0.1", p2),
                new Peer("node3", "127.0.0.1", p3)
        );
        for (int i = 0; i < 3; i++) {
            ClusterNode node = new ClusterNode(configFor(i));
            nodes.add(node);
            node.start();
        }
    }

    private NodeConfig configFor(int index) {
        Peer self = peers.get(index);
        return new NodeConfig(
                self.nodeId(),
                "127.0.0.1",
                self.port(),
                peers,
                tempDir.resolve(self.nodeId()),
                3,
                2,
                2,
                32,
                300
        );
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not allocate a test port", ex);
        }
    }

    private static void await(BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(8));
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting", ex);
            }
        }
        throw new AssertionError("Timed out waiting for condition");
    }
}
