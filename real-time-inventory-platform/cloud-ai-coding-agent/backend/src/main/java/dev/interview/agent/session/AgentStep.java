package dev.interview.agent.session;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="agent_steps",uniqueConstraints=@UniqueConstraint(columnNames={"sessionId","sequenceNo"})) public class AgentStep {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false) private UUID sessionId; @Column(nullable=false) private int sequenceNo; @Column(nullable=false) private String type; @Column(nullable=false) private String status; @Column(length=20000) private String inputJson; @Column(length=50000) private String outputText; private long durationMs; private Instant createdAt;
 protected AgentStep(){} public AgentStep(UUID s,int n,String t,String status,String in,String out,long ms,Instant at){sessionId=s;sequenceNo=n;type=t;this.status=status;inputJson=in;outputText=out;durationMs=ms;createdAt=at;}
 public Long getId(){return id;} public UUID getSessionId(){return sessionId;} public int getSequenceNo(){return sequenceNo;} public String getType(){return type;} public String getStatus(){return status;} public String getInputJson(){return inputJson;} public String getOutputText(){return outputText;} public long getDurationMs(){return durationMs;} public Instant getCreatedAt(){return createdAt;}
}
