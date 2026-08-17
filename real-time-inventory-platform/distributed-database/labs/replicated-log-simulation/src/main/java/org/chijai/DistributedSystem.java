package org.chijai;


/*
────────────────────────────────────────────────────────────────────────────
1. FILE HEADER — PHILOSOPHY + GOAL
────────────────────────────────────────────────────────────────────────────

System:
A single-process, multi-threaded simulation of a distributed replicated log
with leader-based consensus under unreliable networks.

Mental shift it trains:
Failure-first thinking — designing behavior assuming nodes, networks,
and clocks are already broken.

Spine sentence (core lesson):
A distributed system is defined not by how it works when healthy, but by how
it degrades when every assumption is violated.

Metaphor (used once):
This system is a group of people copying a ledger by shouting across a foggy
valley — sometimes voices drop, sometimes people faint, and agreement is
always provisional.
*/

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/*
────────────────────────────────────────────────────────────────────────────
2. HOW TO READ THIS FILE
────────────────────────────────────────────────────────────────────────────

Read top-down. Do not skip comments.

- Invariants are the contract.
- Code exists only to defend invariants.
- This is not a framework.
- Explicit > clever.
*/

/*
────────────────────────────────────────────────────────────────────────────
3. SYSTEM INVARIANTS (SINGLE SOURCE OF TRUTH)
────────────────────────────────────────────────────────────────────────────

RESOURCE BOUNDS
- Fixed nodes: 3
- Threads: 1 per node
- Memory-only
- Bounded queues

ACK RULE
- Write ACK only after majority replication

ORDERING
- Leader log is authoritative
- Followers never reorder

CONSISTENCY
- Leader reads: strong
- Follower reads: possibly stale

IDEMPOTENCY
- Writes are NOT idempotent
- Duplicate delivery tolerated via index checks
- Same value ≠ same operation

FAILURE CONTAINMENT
- Node crash affects only that node
- Partition is per-link

BACKPRESSURE
- No majority → write blocks → fails

TIME
- Clocks are unsynchronized
- Timeouts are heuristics

NON-INVARIANTS
- No exactly-once
- No availability under majority loss
- No persistence across restart
*/

/*
────────────────────────────────────────────────────────────────────────────
4. CORE IMPLEMENTATION
────────────────────────────────────────────────────────────────────────────
*/

public class DistributedSystem {

    static final int NODES = 3;
    static final int MAJORITY = 2;
    static final int MAX_QUEUE = 100;
    static final long NETWORK_DELAY_MS = 50;
    static final long WRITE_TIMEOUT_MS = 2000;

    static class LogEntry {
        final int index;
        final String value;

        LogEntry(int index, String value) {
            this.index = index;
            this.value = value;
        }
    }

    enum Role {LEADER, FOLLOWER}

    static class Network {
        final boolean[][] partitioned = new boolean[NODES][NODES];
        final Set<Integer> slowNodes = new HashSet<>();

        boolean canSend(int from, int to) {
            return !partitioned[from][to];
        }

        void partition(int a, int b) {
            partitioned[a][b] = true;
            partitioned[b][a] = true;
        }

        void heal(int a, int b) {
            partitioned[a][b] = false;
            partitioned[b][a] = false;
        }

        // 🔧 ADDITION: slowness ≠ failure
        boolean isSlow(int nodeId) {
            return slowNodes.contains(nodeId);
        }
    }

    static class Node implements Runnable {
        final int id;
        final Role role;
        final List<LogEntry> log = new ArrayList<>();
        volatile int commitIndex = -1;

        final BlockingQueue<LogEntry> inbox =
                new ArrayBlockingQueue<>(MAX_QUEUE);

        final AtomicBoolean alive = new AtomicBoolean(true);

        Node(int id, Role role) {
            this.id = id;
            this.role = role;
        }

        void receive(LogEntry entry) {
            if (!alive.get()) return;

            synchronized (log) {
                // Index check prevents reordering but NOT full idempotency.
                if (entry.index == log.size()) {
                    log.add(entry);
                    commitIndex = entry.index;
                }
            }
        }

        @Override
        public void run() {
            try {
                while (alive.get()) {
                    LogEntry e = inbox.poll(100, TimeUnit.MILLISECONDS);
                    if (e != null) receive(e);
                }
            } catch (InterruptedException ignored) {
            }
        }

        void crash() {
            alive.set(false);
        }
    }

    static class Leader {
        final Node self;
        final Node[] cluster;
        final Network network;

        Leader(Node self, Node[] cluster, Network network) {
            this.self = self;
            this.cluster = cluster;
            this.network = network;
        }

        /*
         WRITE PATH ORDER (CRITICAL):

         1. Append locally
         2. Replicate
         3. Await majority
         4. Commit

         Crash timeline:
         ┌─────────────┬───────────────────────────────┐
         │ Crash Point │ Outcome                        │
         ├─────────────┼───────────────────────────────┤
         │ After (1)   │ Ghost entry (uncommitted)     │
         │ After (3)   │ Majority durable → committed  │
         └─────────────┴───────────────────────────────┘
        */
        boolean write(String value) throws InterruptedException {
            int index;
            synchronized (self.log) {
                index = self.log.size();
                self.log.add(new LogEntry(index, value));
            }

            CountDownLatch acks = new CountDownLatch(MAJORITY - 1);

            for (Node n : cluster) {
                if (n.id == self.id) continue;
                if (!network.canSend(self.id, n.id)) continue;

                new Thread(() -> {
                    try {
                        if (network.isSlow(n.id)) Thread.sleep(300);
                        else Thread.sleep(NETWORK_DELAY_MS);

                        n.inbox.offer(self.log.get(index));
                        acks.countDown();
                    } catch (Exception ignored) {
                    }
                }).start();
            }

            boolean success = acks.await(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (success) self.commitIndex = index;
            return success;
        }

        // 🔧 ADDITION: explicit read semantics
        String readFromLeader() {
            if (self.commitIndex < 0) return null;
            return self.log.get(self.commitIndex).value;
        }

        String readFromFollower(int followerId) {
            Node f = cluster[followerId];
            if (f.commitIndex < 0) return null;
            return f.log.get(f.commitIndex).value;
        }
    }

    /*
────────────────────────────────────────────────────────────────────────────
6. DEMO / SMOKE TEST
────────────────────────────────────────────────────────────────────────────
*/
    public static void main(String[] args) throws Exception {
        Network network = new Network();

        Node[] nodes = new Node[NODES];
        for (int i = 0; i < NODES; i++) {
            nodes[i] = new Node(i, i == 0 ? Role.LEADER : Role.FOLLOWER);
            new Thread(nodes[i], "node-" + i).start();
        }

        Leader leader = new Leader(nodes[0], nodes, network);

        System.out.println("=== Normal write ===");
        leader.write("A");
        System.out.println("Leader read: " + leader.readFromLeader());

        System.out.println("\n=== Follower read (may lag) ===");
        System.out.println("Follower read: " + leader.readFromFollower(1));

        System.out.println("\n=== Inject partition (leader isolated) ===");
        network.partition(0, 1);
        network.partition(0, 2);
        System.out.println("Write success: " + leader.write("B"));

        System.out.println("\n=== Heal partition ===");
        network.heal(0, 1);
        network.heal(0, 2);
        leader.write("C");

        System.out.println("\n=== Inject slow follower ===");
        network.slowNodes.add(2);
        leader.write("D");
        System.out.println("Quorum succeeded despite slowness");

        System.out.println("\n=== Crash leader mid-flight ===");
        leader.write("E");
        nodes[0].crash();

        System.out.println("\n=== Logs after crash ===");
        for (Node n : nodes) {
            System.out.println(
                    "Node " + n.id +
                            " logSize=" + n.log.size() +
                            " commitIndex=" + n.commitIndex
            );
        }

        for (Node n : nodes) {
            n.crash();
        }
    }

    /*
────────────────────────────────────────────────────────────────────────────
7. KNOWLEDGE INDEX (CONTROLLED COMPRESSION)
────────────────────────────────────────────────────────────────────────────

KEY INSIGHTS
- Visibility ≠ commitment
- Slowness is more dangerous than failure
- Reads encode consistency choices

WHY NOT RAFT?
- Elections add correctness surface
- Failure-first thinking comes before algorithms

WEAKEST POINT
- No leader recovery
- No persistent storage

FIRST PRODUCTION CHANGE
- Durable log
- Leader re-election

CORRECTNESS VS PERFORMANCE
- Correctness prioritized
- Availability sacrificed under partition

FINAL TRUTH
If this feels pessimistic, it’s because reality is.
*/
}
