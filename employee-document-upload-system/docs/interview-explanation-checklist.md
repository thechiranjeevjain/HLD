# Interview Explanation Checklist

Use this checklist to prove you understand the infrastructure.

## Architecture

- Explain why the API is stateless.
- Explain why the load balancer is public but ECS tasks are private.
- Explain why RDS is in private data subnets.
- Explain why S3 stores files and PostgreSQL stores metadata.
- Explain how upload URLs avoid pushing large file bytes through the API.

## Security

- Distinguish authentication, application RBAC, and IAM.
- Describe the four app roles and one action each role cannot perform.
- Show where least privilege is enforced in Terraform.
- Explain why the S3 bucket is private even though browsers upload to it.
- Explain how KMS, Secrets Manager, and security groups reduce blast radius.

## Operations

- Name the first three dashboards you would check during an incident.
- Explain which alarm tells you users are seeing server errors.
- Explain what log fields are needed to trace a single failed upload.
- Explain how a bad deployment rolls back.

## Cost

- Identify the likely small-scale cost surprises: RDS and NAT gateway.
- Explain how log retention affects monthly spend.
- Explain why S3 lifecycle rules can reduce storage cost but not always total cost.
- Explain the difference between dev and prod cost posture.

## Backups

- Define RPO and RTO for this app.
- Explain why RDS backup and S3 versioning solve different problems.
- Walk through restoring metadata to a new RDS instance.
- Explain why "backup enabled" is weaker than "restore tested."

## CI/CD And IaC

- Explain why Terraform is reviewed through plan output before apply.
- Explain why GitHub Actions should assume AWS roles through OIDC instead of static long-lived AWS keys.
- Explain how environments map to AWS accounts or workspaces.
- Explain why production apply should require approval.
