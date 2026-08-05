package org.chijai;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. FILE HEADER (PHILOSOPHY + GOAL)
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * What system this is:
 *   A single-process, single-node message queue with producers, consumers,
 *   offsets, retries, and explicit failure semantics.
 *
 * What mental shift it trains:
 *   Temporal decoupling — learning to reason when cause and effect are separated
 *   in time, retries exist, and duplication is normal.
 *
 * ONE spine sentence (core lesson):
 *   Sending a message is an expression of intent, not a guarantee of execution.
 *
 * ONE simple metaphor (used once only):
 *   Dropping letters into a postbox does not mean they are read once, immediately,
 *   or even at all.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * 2. HOW TO READ THIS FILE
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Read top-down, slowly.
 *
 * This file is intentionally verbose and judgment-heavy.
 * Comments are not decoration — they are the system.
 *
 * Every invariant is declared before code exists.
 * Every line of code must defend at least one invariant.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * 3. SYSTEM INVARIANTS (SINGLE SOURCE OF TRUTH)
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * EXPLICIT INVARIANTS (WHAT IS GUARANTEED)
 *
 * I1. Durability (Process-Lifetime)
 *     - Once a message is enqueued, it remains in memory until explicitly
 *       acknowledged (acked) or the process terminates.
 *
 * I2. Ordering (Per-Queue)
 *     - Messages are assigned monotonically increasing offsets.
 *     - Delivery attempts respect offset order.
 *
 * I3. Visibility vs Acknowledgment
 *     - A message may be delivered multiple times.
 *     - Acknowledgment is the ONLY removal signal.
 *
 * I4. At-Least-Once Delivery
 *     - Messages are delivered one or more times.
 *
 * I5. Failure Containment
 *     - Consumer failures do not crash the queue.
 *
 * NON-INVARIANTS
 * - Exactly-once delivery
 * - Fairness
 * - Crash recovery
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageQueue {

    /* ─────────────────────────────────────────────────────────────
     * DATA MODEL
     * ─────────────────────────────────────────────────────────────
     */

    static final class Message {
        final long offset;
        final String payload;

        Message(long offset, String payload) {
            this.offset = offset;
            this.payload = payload;
        }
    }

    static final class DeliveryState {
        boolean acknowledged = false;
        int attemptCount = 0;
        long nextEligibleTimeNanos = 0L;
    }

    static final class Record {
        final Message message;
        final DeliveryState state = new DeliveryState();

        Record(Message message) {
            this.message = message;
        }
    }

    /* ─────────────────────────────────────────────────────────────
     * CORE ENGINE
     * ─────────────────────────────────────────────────────────────
     */

    private final int capacity;
    private final List<Record> log = new ArrayList<>();
    private final List<Record> deadLetterLog = new ArrayList<>();
    private final AtomicLong nextOffset = new AtomicLong(0);

    private volatile Consumer consumer;
    private final Thread deliveryThread;
    private volatile boolean running = true;

    private static final long RETRY_DELAY_NANOS = 500_000_000L; // 500ms
    private static final int MAX_ATTEMPTS = 3;

    public MessageQueue(int capacity) {
        this.capacity = capacity;
        this.deliveryThread = new Thread(this::deliveryLoop, "delivery-loop");
        this.deliveryThread.start();
    }

    /* ─────────────────────────────────────────────────────────────
     * WRITE / MUTATION PATH
     * ─────────────────────────────────────────────────────────────
     *
     * Order:
     * 1. Capacity check
     * 2. Offset assignment
     * 3. Append
     *
     * Reversing this order breaks offset continuity or leaks capacity.
     */

    public synchronized void produce(String payload) {
        if (log.size() >= capacity) {
            throw new IllegalStateException("Queue capacity exceeded");
        }

        long offset = nextOffset.getAndIncrement();
        log.add(new Record(new Message(offset, payload)));
    }

    public void registerConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    /* ─────────────────────────────────────────────────────────────
     * DELIVERY LOOP
     * ─────────────────────────────────────────────────────────────
     *
     * IMPORTANT CONCURRENCY NOTE:
     * Consumer execution happens INSIDE this single thread.
     *
     * Parallelizing delivery seems attractive for throughput,
     * but it destroys:
     * - Offset reasoning
     * - Failure timelines
     * - Deterministic retries
     */

    private void deliveryLoop() {
        while (running) {
            try {
                deliverOnce();
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void deliverOnce() {
        if (consumer == null) return;

        long now = System.nanoTime();

        for (Iterator<Record> it = log.iterator(); it.hasNext(); ) {
            Record record = it.next();
            DeliveryState state = record.state;

            if (state.acknowledged) continue;
            if (now < state.nextEligibleTimeNanos) continue;

            state.attemptCount++;

            if (state.attemptCount > MAX_ATTEMPTS) {
                deadLetterLog.add(record);
                it.remove();
                System.out.println(
                        "DLQ offset=" + record.message.offset +
                                " payload=" + record.message.payload
                );
                continue;
            }

            try {
                consumer.consume(
                        record.message,
                        () -> state.acknowledged = true,
                        state.attemptCount
                );
            } catch (Exception e) {
                state.nextEligibleTimeNanos = now + RETRY_DELAY_NANOS;
            }
        }
    }

    /* ─────────────────────────────────────────────────────────────
     * MAINTENANCE
     * ─────────────────────────────────────────────────────────────
     */

    public synchronized void compact() {
        Iterator<Record> it = log.iterator();
        while (it.hasNext()) {
            if (it.next().state.acknowledged) {
                it.remove();
            } else {
                break;
            }
        }
    }

    public void shutdown() throws InterruptedException {
        running = false;
        deliveryThread.join();
    }

    /* ─────────────────────────────────────────────────────────────
     * SUPPORTING COMPONENTS
     * ─────────────────────────────────────────────────────────────
     */

    public interface Consumer {
        void consume(Message message, Ack ack, int attempt);
    }

    public interface Ack {
        void acknowledge();
    }

    /* ─────────────────────────────────────────────────────────────
     * DEMO / SMOKE TEST
     * ─────────────────────────────────────────────────────────────
     */

    public static void main(String[] args) throws Exception {
        MessageQueue queue = new MessageQueue(10);

        queue.registerConsumer(new Consumer() {

            private final Set<Long> failedOnce = new HashSet<>();
            private final Set<Long> sideEffects = new HashSet<>();

            @Override
            public void consume(Message message, Ack ack, int attempt) {
                System.out.println(
                        "Consumed offset=" + message.offset +
                                " attempt=" + attempt +
                                " payload=" + message.payload
                );

                if ("poison".equals(message.payload)) {
                    throw new RuntimeException("Simulated poison message");
                }

                if (failedOnce.add(message.offset)) {
                    throw new RuntimeException("Simulated failure");
                }

                sideEffects.add(message.offset);
                // This ack does NOT mean exactly-once.
                // It only means "stop retrying".
                ack.acknowledge();
                System.out.println("Acked offset=" + message.offset + " sideEffects=" + sideEffects.size());
            }
        });

        queue.produce("order-1");
        queue.produce("poison");
        queue.produce("order-2");

        Thread.sleep(2500);

        queue.compact();
        queue.shutdown();
    }

    /* ─────────────────────────────────────────────────────────────
     * KNOWLEDGE INDEX (CONTROLLED COMPRESSION)
     * ─────────────────────────────────────────────────────────────
     *
     * - Offsets ≠ correctness
     * - Retries create duplicates
     * - Idempotency belongs to business logic
     * - DLQs are liveness valves
     * - Exactly-once is a coordination illusion
     *
     * Weakest point:
     * - No crash recovery
     *
     * First production change:
     * - Persistent log + idempotency keys
     *
     * ─────────────────────────────────────────────────────────────
     */
}

