# Employee Document Upload System Demo Script

## Local Code Demo

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\employee-document-upload-system\app
mvn test
```

Run locally after PostgreSQL and JWT issuer settings are configured:

```powershell
mvn spring-boot:run
```

## API Walkthrough

Use these endpoints as the explanation path:

```text
GET  /actuator/health
POST /api/documents/upload-intents
GET  /api/documents
POST /api/documents/{id}/review
```

## Infrastructure Walkthrough

1. Start with identity: Cognito authenticates users and groups map to app roles.
2. Explain upload intent: API authorizes and returns a short-lived S3 upload URL.
3. Explain storage split: S3 stores bytes, PostgreSQL stores metadata.
4. Explain privacy: private subnets, blocked public S3 access, security groups, and least privilege.
5. Explain operations: CloudWatch, CloudTrail, alarms, backups, and restore drills.
6. Explain Terraform: review `terraform plan` before applying any cost-incurring resources.

## Interview Close

Say: this project is not about complex endpoints. It is about designing a secure, auditable, operable path for sensitive files.
