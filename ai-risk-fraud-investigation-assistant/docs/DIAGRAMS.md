# Secure AI Risk Analyst Agent diagrams

## High-Level Design

### Security architecture and trust boundaries

```mermaid
flowchart LR
    subgraph U["Untrusted zone"]
        User["Analyst prompt"]
        Doc["Retrieved document"]
    end
    subgraph C["Deterministic control plane"]
        Auth["Authentication and RBAC"]
        Guard["Injection inspection"]
        Policy["Risk and policy engine"]
        Gateway["Read-tool allowlist"]
        DLP["Output schema and DLP"]
        Approval["Human approval gate"]
    end
    subgraph M["Untrusted compute"]
        LLM["LLM planner"]
    end
    subgraph D["Protected data plane"]
        Evidence[("Masked evidence")]
        CaseDB[("Case database")]
        Audit[("Append-only audit")]
    end
    User --> Auth --> Guard
    Doc --> Guard
    Guard -->|safe content only| LLM
    Guard -->|deny| Audit
    LLM --> Policy --> Gateway --> Evidence
    Evidence --> LLM
    LLM --> DLP
    DLP -->|recommendation only| Approval
    DLP -->|deny| Audit
    Approval -->|authorized action| CaseDB
    Gateway --> Audit
    Approval --> Audit
```

## Low-Level Design

### Investigation and privileged-action sequence

```mermaid
sequenceDiagram
    actor Analyst
    participant API as Case API
    participant Guard as Security guard
    participant AI as LLM orchestrator
    participant Policy as Risk/policy engine
    participant Tools as Read-only gateway
    participant Audit as Durable audit
    participant Senior as Senior analyst
    Analyst->>API: investigate(case, prompt, document)
    API->>Guard: inspect untrusted content
    alt injection detected
        Guard->>Audit: commit denial independently
        Guard-->>Analyst: 403 BLOCK
    else content accepted
    Guard->>AI: sanitized context
    AI->>Policy: evaluate risk and permitted operations
    Policy->>Tools: approved read calls
    AI->>Tools: read approved evidence
    Tools-->>AI: masked evidence
    AI->>Guard: validate structured output and secrets
    Guard-->>Analyst: recommendation and citations
    Analyst->>Senior: request privileged action
    Senior->>API: approve with rationale and version
    API->>Audit: record named approver and decision
    end
```

### Unauthorized tool-call path

```mermaid
flowchart TD
    Model["Model requests a tool"] --> Match{"Exact allowlist match?"}
    Match -->|yes| Read["Execute scoped read"]
    Read --> Mask["Mask result"]
    Mask --> Log["Audit tool result"]
    Match -->|no| Deny["Throw SecurityException"]
    Deny --> DenialLog["Commit UNAUTHORIZED_TOOL_CALL_BLOCKED"]
    DenialLog --> NoWrite["No privileged side effect"]
```

### Adversarial evaluation matrix

```mermaid
flowchart LR
    Tests["AdversarialAgentSecurityTest"] --> P["Prompt injection"]
    Tests --> D["Malicious document"]
    Tests --> T["Unauthorized write tool"]
    Tests --> E["Secret exfiltration"]
    P --> Block["Blocked and audited"]
    D --> Block
    T --> Block
    E --> Block
```

### Portfolio boundary

```mermaid
flowchart TB
    FraudPlatform["fraud-detection-platform"] --> Scoring["Fast scoring and velocity checks"]
    FraudPlatform --> DecisionRead["Decision lookup"]
    Assistant["ai-risk-fraud-investigation-assistant"] --> Casework["Investigation case workflow"]
    Assistant --> HumanReview["Human approval and audit"]
    Assistant --> EvidenceAI["Evidence-grounded AI summary"]
```
