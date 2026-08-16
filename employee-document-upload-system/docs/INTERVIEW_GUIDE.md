# Employee Document Upload System Interview Guide

## Two-Minute Pitch

This project explains a secure employee document upload system. The API is intentionally small; the main learning value is the surrounding cloud architecture: Cognito identity, application RBAC, private S3 uploads, PostgreSQL metadata, KMS encryption, audit logging, monitoring, CI/CD, Terraform, cost controls, and backup strategy.

## What To Emphasize

- File bytes go directly to S3 using short-lived signed upload URLs.
- Metadata and review state live in PostgreSQL.
- Cognito authenticates users; application RBAC authorizes actions.
- S3 and RDS are private and encrypted with KMS.
- CloudWatch and CloudTrail provide operational and audit evidence.
- Terraform makes infrastructure reviewable before deployment.

## Request Flow

1. Employee signs in and receives a JWT.
2. API validates the JWT and maps groups to roles.
3. Employee requests an upload intent.
4. API stores document metadata and returns a short-lived S3 upload contract.
5. Employee uploads file bytes directly to private S3.
6. HR reviewer approves or rejects metadata.
7. Auditor can inspect state without modifying documents.

## Tradeoffs

| Decision            | Benefit                               | Cost                                              |
| ------------------- | ------------------------------------- | ------------------------------------------------- |
| Direct S3 upload    | API avoids large file transfer load   | Client must handle signed URL upload correctly    |
| PostgreSQL metadata | Queryable review state                | Requires schema and backup operations             |
| Cognito groups      | Managed identity and role mapping     | Cloud provider coupling                           |
| Terraform           | Repeatable infra and reviewable plans | State management and cost discipline are required |

## FAQ

Q: Why not upload files through the API?
A: Large file bytes would consume API bandwidth, memory, and scaling capacity. Signed S3 upload keeps the API focused on authorization and metadata.

Q: Why split metadata and file storage?
A: PostgreSQL is good for queryable state; S3 is good for durable object bytes.

Q: What would you add next?
A: malware scanning, event-driven S3 callbacks, retention policies, legal hold, restore drills, and full Cognito integration tests.
