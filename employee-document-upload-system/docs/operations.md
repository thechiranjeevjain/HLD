# Logging, Monitoring, And Operations

## Logs

Application logs should be structured JSON in production. Each request should include:

- `correlationId`
- `userSubject`
- `action`
- `documentId`
- `decision`
- `latencyMs`
- `errorCode`

CloudWatch log groups:

- `/ecs/employee-docs-api`
- `/aws/rds/instance/<db-id>/postgresql`
- CloudTrail destination log group or S3 bucket, depending on retention needs.

## Metrics

Minimum dashboard:

| Area | Metric |
| --- | --- |
| Load balancer | Request count, target 5xx, target response time |
| ECS | Running task count, CPU, memory |
| API | Request latency, upload-intent count, authorization deny count |
| RDS | CPU, free storage, connections, read/write latency |
| S3 | 4xx/5xx errors, bucket size, object count |
| Backups | Backup job success/failure |

## Alarms

Create alarms for:

- ALB target 5xx count greater than zero for sustained periods.
- ECS CPU above 80 percent.
- RDS CPU above 80 percent.
- RDS free storage below the operational threshold.
- Backup job failure.
- KMS access denied spikes.

## Runbook: High 5xx Rate

1. Check ALB target health.
2. Check ECS service events for task restarts or failed deployments.
3. Inspect recent CloudWatch logs by correlation id and error code.
4. Check RDS CPU, connections, and storage.
5. Roll back the ECS task definition if errors started after deployment.
6. Open an incident note with timeline, blast radius, and customer impact.

## Runbook: Employee Cannot Upload

1. Confirm the user can sign in to Cognito.
2. Decode token claims and verify the expected group exists.
3. Check API authorization deny logs.
4. Check RDS write availability.
5. Check S3/KMS denied events in CloudTrail.
6. Confirm upload URL TTL did not expire before client upload.

## Runbook: Restore Database

1. Identify target restore time and required RPO/RTO.
2. Restore RDS to a new instance from automated backup or AWS Backup recovery point.
3. Run migration validation and row-count sanity checks.
4. Point a staging API task at the restored database.
5. Validate document metadata against S3 object existence.
6. Promote by changing application database secret or endpoint only after approval.

## Deployment Strategy

The starter CI/CD workflow:

- Runs Maven tests.
- Builds a container image.
- Publishes the image to ECR on `main`.
- Runs Terraform formatting, validation, and plan.

For production, use:

- Separate dev/stage/prod AWS accounts.
- GitHub OIDC to assume deploy roles.
- Required approvals for production Terraform apply.
- Blue/green or rolling ECS deployments with health checks.
- Deployment rollback when alarms fire.
