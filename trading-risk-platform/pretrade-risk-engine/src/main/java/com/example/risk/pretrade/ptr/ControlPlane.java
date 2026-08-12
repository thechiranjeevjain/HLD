package com.example.risk.pretrade.ptr;

import com.example.risk.pretrade.ptr.PtrCore.Limits;
import com.example.risk.pretrade.ptr.PtrCore.LimitsProvider;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/** Deliberately outside the risk event-loop: configuration, authorization and HA coordination. */
public final class ControlPlane {
    private ControlPlane() {}
    public enum Lifecycle { NEW, ADDED, STAGED, COMMITTED }
    public record RiskConfig(String id,long version,Lifecycle lifecycle,Limits limits,String writer,String eventId) {
        public RiskConfig transition(Lifecycle next){if(next.ordinal()!=lifecycle.ordinal()+1)throw new IllegalStateException("invalid lifecycle "+lifecycle+" -> "+next);return new RiskConfig(id,version,next,limits,writer,eventId);}
    }
    public interface CacheListener<K,V>{default void added(K k,V v){} default void updated(K k,V old,V v){} default void deleted(K k,V old){}}
    public static final class ListenableCache<K,V>{
        private final ConcurrentMap<K,V> values=new ConcurrentHashMap<>();private final CopyOnWriteArrayList<CacheListener<K,V>> listeners=new CopyOnWriteArrayList<>();
        public void listen(CacheListener<K,V> l){listeners.add(l);} public V get(K k){return values.get(k);} public Collection<V> values(){return List.copyOf(values.values());}
        public void put(K k,V v){V old=values.put(k,v);if(old==null)listeners.forEach(l->l.added(k,v));else listeners.forEach(l->l.updated(k,old,v));}
        public void delete(K k){V old=values.remove(k);if(old!=null)listeners.forEach(l->l.deleted(k,old));}
    }
    /** Observer-maintained index: hot path reads one volatile reference, never scans config. */
    public static final class CommittedLimitStore implements CacheListener<String,RiskConfig>, LimitsProvider {
        private volatile Limits current=new Limits(10_000,10_000,500_000_0000L,1_000,10_000);
        public Limits current(){return current;} public void added(String k,RiskConfig v){accept(v);}public void updated(String k,RiskConfig o,RiskConfig v){accept(v);}private void accept(RiskConfig v){if(v.lifecycle==Lifecycle.COMMITTED)current=v.limits;}
    }
    public static final class ConfigWriter {
        private final String writerId;private final ListenableCache<String,RiskConfig> cache;private final Set<String> events=ConcurrentHashMap.newKeySet();
        public ConfigWriter(String writerId,ListenableCache<String,RiskConfig> cache){this.writerId=writerId;this.cache=cache;}
        public synchronized RiskConfig apply(RiskConfig candidate){if(!writerId.equals(candidate.writer))throw new SecurityException("not authoritative writer");if(!events.add(candidate.eventId))return cache.get(candidate.id);RiskConfig old=cache.get(candidate.id);if(old!=null&&candidate.version<old.version)throw new IllegalArgumentException("version cannot decrease");if(old!=null&&candidate.version==old.version&&candidate.lifecycle.ordinal()!=old.lifecycle.ordinal()+1)throw new IllegalArgumentException("same version must advance exactly one lifecycle stage");cache.put(candidate.id,candidate);return candidate;}
    }

    public enum Permission { VIEW, EDIT, DELEGATE }
    public record Ace(String entity,String principal,Set<Permission> permissions,String delegatedBy){}
    public static final class AclService {
        private final Map<String,List<Ace>> aces=new ConcurrentHashMap<>();
        public void grant(String actor,Ace ace){if(ace.delegatedBy!=null&&!ace.delegatedBy.equals(actor))throw new SecurityException("delegator mismatch");if(!actor.equals("admin")&&!allowed(ace.entity,actor,Permission.DELEGATE))throw new SecurityException("no delegation permission");aces.computeIfAbsent(ace.entity,k->new CopyOnWriteArrayList<>()).add(ace);}
        public boolean allowed(String entity,String principal,Permission p){return aces.getOrDefault(entity,List.of()).stream().anyMatch(a->a.principal.equals(principal)&&a.permissions.contains(p));}
    }

    public interface LeaseStore { boolean acquire(String node,Instant until); boolean renew(String node,Instant until); String leader(); void release(String node); }
    public static final class InMemoryLeaseStore implements LeaseStore {
        private String leader;private Instant until=Instant.EPOCH;private final Clock clock;public InMemoryLeaseStore(Clock clock){this.clock=clock;}
        public synchronized boolean acquire(String node,Instant expiry){if(leader==null||clock.instant().isAfter(until)||leader.equals(node)){leader=node;until=expiry;return true;}return false;}
        public synchronized boolean renew(String node,Instant expiry){if(!Objects.equals(leader,node)||clock.instant().isAfter(until)){return false;}until=expiry;return true;}
        public synchronized String leader(){return clock.instant().isAfter(until)?null:leader;} public synchronized void release(String node){if(Objects.equals(leader,node)){leader=null;until=Instant.EPOCH;}}
    }
    public static final class LeaderElector {
        private final String node;private final LeaseStore store;private final Clock clock;private final Duration ttl;private final AtomicBoolean primary=new AtomicBoolean();
        public LeaderElector(String node,LeaseStore store,Clock clock,Duration ttl){this.node=node;this.store=store;this.clock=clock;this.ttl=ttl;}
        public boolean campaign(){boolean won=store.acquire(node,clock.instant().plus(ttl));primary.set(won);return won;}
        public void assertPrimary(){if(!primary.get()||!node.equals(store.leader())){primary.set(false);throw new IllegalStateException("split-brain guard: node has no valid lease");}}
        public boolean heartbeat(){boolean ok=store.renew(node,clock.instant().plus(ttl));primary.set(ok);return ok;} public void stop(){store.release(node);primary.set(false);} public boolean isPrimary(){return primary.get();}
    }
}
