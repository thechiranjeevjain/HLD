package dev.interview.agent.session;
public enum SessionState { CREATED,ALLOCATING,CLONING,PLANNING,EXECUTING,VALIDATING,COMPLETED,FAILED,CANCELLED,TIMED_OUT;
 public boolean terminal(){return this==COMPLETED||this==FAILED||this==CANCELLED||this==TIMED_OUT;}
}
