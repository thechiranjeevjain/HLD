# Cost And Backup Strategy

Pricing must be verified in the target AWS region before deployment. These notes were prepared on 2026-08-05 using AWS public pricing pages, but the correct operational habit is to model the workload in the AWS Pricing Calculator and review the Terraform plan before applying.

## Main Cost Drivers

| Service                   | Why it costs money                                    | Cost control                                              |
| ------------------------- | ----------------------------------------------------- | --------------------------------------------------------- |
| ECS Fargate               | vCPU and memory while tasks run                       | Right-size tasks, autoscale, stop non-prod after hours    |
| RDS PostgreSQL            | Instance hours, storage, backups, Multi-AZ            | Use small classes in dev, Multi-AZ only where required    |
| S3                        | Stored GB, requests, retrieval, lifecycle transitions | Lifecycle old versions, avoid unnecessary duplicate files |
| NAT Gateway               | Hourly gateway charge and data processing             | Prefer VPC endpoints for AWS APIs; use one NAT in dev     |
| CloudWatch                | Logs ingestion/storage, custom metrics, alarms        | Retention policies, avoid verbose debug logs in prod      |
| CloudTrail S3 data events | Per-event charges at volume                           | Enable selectively for sensitive prefixes                 |
| AWS Backup                | Backup storage, restore, cross-region copy            | Tier retention by environment and compliance need         |

## Learning-Environment Cost Posture

For a small learning deployment:

- One ECS service with one small Fargate task.
- One small RDS PostgreSQL instance.
- One S3 bucket.
- One NAT gateway.
- Short CloudWatch log retention.
- No cross-region backup copy unless specifically testing disaster recovery.

The NAT gateway and always-on RDS instance are usually the surprise costs at small scale. For a cheaper learning version, run the API locally, keep only S3/Cognito/IAM in AWS, or replace NAT with VPC endpoints where practical.

## Production Cost Posture

For production:

- Use Multi-AZ RDS if the recovery target requires it.
- Use at least two ECS tasks across Availability Zones.
- Use VPC endpoints for S3, ECR, CloudWatch Logs, Secrets Manager, and KMS where traffic volume justifies it.
- Set log retention by data classification.
- Add AWS Budgets alerts per environment.
- Tag all resources with `Project`, `Environment`, and `Owner`.

## Backup Strategy

| Data             | Backup mechanism                            | Default retention                                   | Restore test                        |
| ---------------- | ------------------------------------------- | --------------------------------------------------- | ----------------------------------- |
| RDS metadata     | Automated backups plus AWS Backup plan      | 7 days dev, 35 days prod candidate                  | Monthly restore to staging          |
| S3 documents     | Versioning, optional replication, lifecycle | Keep current, expire noncurrent after policy window | Sample object restore and checksum  |
| Terraform state  | Remote encrypted backend with locking       | Keep versioned state bucket                         | Recover state from versioned object |
| Container images | ECR lifecycle policy                        | Keep last 20 images                                 | Redeploy previous image tag         |

## RPO And RTO

Suggested targets for the learning project:

- RPO: 24 hours for metadata in dev, 1 hour or less for production candidate.
- RTO: 4 hours for dev, 1 hour for production candidate.

Explain that RPO is how much data loss is acceptable, while RTO is how long recovery can take.

## Backup Caveats

- Backups are not complete until restore has been tested.
- RDS automated backups protect metadata, not S3 object bytes.
- S3 versioning helps with accidental overwrite/delete, but it also increases storage cost.
- Long retention can be expensive if users upload large documents frequently.
- Cross-region backups improve disaster recovery but add storage and transfer costs.

## Source Notes

- Amazon S3 pricing: https://aws.amazon.com/s3/pricing/
- Amazon RDS for PostgreSQL pricing: https://aws.amazon.com/rds/postgresql/pricing/
- Amazon CloudWatch pricing: https://aws.amazon.com/cloudwatch/pricing/
- AWS Backup pricing: https://aws.amazon.com/backup/pricing/
- AWS Backup metering and billing: https://docs.aws.amazon.com/aws-backup/latest/devguide/metering-and-billing.html
