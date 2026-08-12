package dev.interview.agent.session;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
interface SessionRepository extends JpaRepository<AgentSession,UUID>{} interface StepRepository extends JpaRepository<AgentStep,Long>{List<AgentStep> findBySessionIdOrderBySequenceNo(UUID id);}
