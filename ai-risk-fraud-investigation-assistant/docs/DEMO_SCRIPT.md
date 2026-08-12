# AI Risk Fraud Investigation Assistant Demo Script

## Verify

Use a JDK that supports release 21:

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\ai-risk-fraud-investigation-assistant
$jdk = "C:\Users\Chiranjeev Jain\.jdks\openjdk-25"
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
mvn verify
cd frontend
npm.cmd run build
```

## Run The Fast Local Demo

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\ai-risk-fraud-investigation-assistant
.\scripts\start-demo.cmd
```

Open `http://localhost:5173`.

Demo users:

- `analyst / analyst-demo`
- `senior / senior-demo`
- `admin / admin-demo`
- `auditor / auditor-demo`

Stop:

```powershell
.\scripts\stop-demo.cmd
```

## Walkthrough

1. Log in as `analyst`.
2. Ingest or open a risky transaction.
3. Show deterministic fraud signals before the AI step.
4. Run investigation and point to allowlisted evidence reads.
5. Show cited policy chunks and structured recommendation output.
6. Attempt a sensitive action and explain why senior approval is required.
7. Log in as `senior`, approve or reject with rationale, and show the audit trail.

## Interview Close

Say: this project is not replacing the deterministic fraud engine. It adds the investigation workflow around it: evidence-grounded AI, policy citations, role boundaries, approval control, and auditability.
