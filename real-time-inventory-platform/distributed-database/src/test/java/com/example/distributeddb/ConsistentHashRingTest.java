package com.example.distributeddb;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentHashRingTest {
    @Test
    void returnsUniqueReplicaSetForKey() {
        List<Peer> peers = List.of(
                new Peer("node1", "127.0.0.1", 9101),
                new Peer("node2", "127.0.0.1", 9102),
                new Peer("node3", "127.0.0.1", 9103)
        );
        ConsistentHashRing ring = new ConsistentHashRing(peers, 32);

        List<Peer> replicas = ring.replicasFor("account:42", 3);

        assertEquals(3, replicas.size());
        assertEquals(3, replicas.stream().map(Peer::nodeId).distinct().count());
        assertTrue(ring.isReplica("account:42", 3, replicas.get(0).nodeId()));
    }
}
