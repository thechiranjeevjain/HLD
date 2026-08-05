package io.exchangelite.engine.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SessionManager {
    private final AtomicLong nextSessionId = new AtomicLong(1);
    private final Map<Long, Long> connectedAtNanosBySession = new ConcurrentHashMap<>();

    public long register() {
        long id = nextSessionId.getAndIncrement();
        connectedAtNanosBySession.put(id, System.nanoTime());
        return id;
    }

    public void unregister(long sessionId) {
        connectedAtNanosBySession.remove(sessionId);
    }

    public int activeSessions() {
        return connectedAtNanosBySession.size();
    }

    public String json() {
        return "{\"activeSessions\":" + activeSessions() + ",\"nextSessionId\":" + nextSessionId.get() + "}";
    }
}
