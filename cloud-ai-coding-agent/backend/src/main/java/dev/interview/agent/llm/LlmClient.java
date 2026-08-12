package dev.interview.agent.llm;
import java.util.*;
public interface LlmClient { PlanResponse plan(String task,String repositoryContext); record PlanResponse(List<Action> actions,String rationale,long inputTokens,long outputTokens){} record Action(String tool,Map<String,String> arguments){} }
