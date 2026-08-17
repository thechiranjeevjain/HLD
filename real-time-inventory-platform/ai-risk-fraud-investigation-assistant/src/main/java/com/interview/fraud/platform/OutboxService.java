package com.interview.fraud.platform;
import java.time.Instant; import java.util.UUID; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service;
@Service public class OutboxService {private final JdbcTemplate jdbc; public OutboxService(JdbcTemplate jdbc){this.jdbc=jdbc;} public void add(String aggregate,String id,String event,String payload){jdbc.update("INSERT INTO outbox_events(id,aggregate_type,aggregate_id,event_type,payload,status,attempts,created_at) VALUES(?,?,?,?,?,'PENDING',0,?)",UUID.randomUUID(),aggregate,id,event,payload,Instant.now());}}
