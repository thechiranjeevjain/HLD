package dev.interview.agent.llm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component; import java.util.*;
@Component @ConditionalOnProperty(name="agent.llm.provider",havingValue="fake",matchIfMissing=true) public class FakeLlmClient implements LlmClient {
 public PlanResponse plan(String task,String context){var actions=new ArrayList<Action>(); actions.add(new Action("list_files",Map.of("path","."))); actions.add(new Action("search_text",Map.of("query","TODO","path","."))); actions.add(new Action("write_file",Map.of("path","AGENT_RESULT.md","content","# Agent result\n\nTask: "+task.replace("\n"," ")+"\n"))); actions.add(new Action("git_status",Map.of())); actions.add(new Action("run_tests",Map.of())); actions.add(new Action("git_diff",Map.of())); return new PlanResponse(actions,"Inspect, make one safe demonstrable change, validate, and report.",Math.max(1,(task.length()+context.length())/4),80);}
}
