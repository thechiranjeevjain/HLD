# Security And RBAC

## Identity Model

Human users authenticate through Amazon Cognito User Pools. Cognito issues JWTs. The API validates those JWTs and reads the `cognito:groups` claim.

Application groups:

- `EMPLOYEE`
- `HR_REVIEWER`
- `AUDITOR`
- `ADMIN`

## RBAC Rules

| Action | EMPLOYEE | HR_REVIEWER | AUDITOR | ADMIN |
| --- | --- | --- | --- | --- |
| Create upload intent for self | Yes | Yes | No | Yes |
| Create upload intent for another user | No | Yes | No | Yes |
| List own metadata | Yes | Yes | Yes | Yes |
| List all metadata | No | Yes | Yes | Yes |
| Approve/reject document | No | Yes | No | Yes |
| Delete document | No | No | No | Yes, with audit trail |
| Change roles | No | No | No | Outside app, through identity administration |

## IAM Model

IAM is used for machine authorization:

- ECS task execution role pulls images, writes logs, and reads injected runtime secrets.
- ECS task role signs S3 document operations and uses KMS for document storage access.
- RDS is not publicly reachable.
- S3 bucket policy denies non-TLS access.
- KMS key policy grants usage only to the application role and service principals that need encryption.

This distinction matters: Cognito roles decide what a user can do inside the app; IAM roles decide what AWS actions the workload can perform.

## Least Privilege Examples

The task role should have only:

- The task role has `s3:PutObject`, `s3:GetObject`, `s3:AbortMultipartUpload` for the document bucket.
- The task role has `kms:Encrypt`, `kms:Decrypt`, `kms:GenerateDataKey` for the document KMS key.
- The execution role has `secretsmanager:GetSecretValue` for injected database credentials.
- The execution role has CloudWatch log permissions through the AWS managed ECS execution policy.

It should not have broad permissions like `s3:*`, `rds:*`, or `iam:*`.

## Private Storage Controls

S3 controls:

- Block public ACLs and public bucket policies.
- Enable bucket versioning.
- Require HTTPS with a bucket policy.
- Encrypt objects using a customer managed KMS key.
- Use lifecycle rules for old versions and incomplete multipart uploads.
- Keep object keys tenant/user scoped, for example `employee/{userId}/{date}/{documentId}-{filename}`.

## Audit Controls

Application audit events should include:

- User subject.
- Role at decision time.
- Document id.
- Requested action.
- Decision result.
- Correlation id.

AWS audit events should include:

- CloudTrail management events.
- S3 data events for sensitive document object access where cost is acceptable.
- KMS access denied events.
- IAM policy changes.

## Threats And Mitigations

| Threat | Mitigation |
| --- | --- |
| Public document exposure | S3 public access block, private bucket, deny non-TLS policy |
| Employee reads another employee document | API RBAC checks owner id against token subject |
| Compromised app role accesses unrelated buckets | IAM policy scoped to one bucket ARN |
| Secrets in CI logs | GitHub OIDC federation, no static AWS keys, masked secrets |
| SQL injection | Parameterized JPA repository operations |
| Accidental data deletion | S3 versioning, RDS backups, deletion protection in prod |
| Missing evidence after incident | Application audit logs plus CloudTrail |

## Source Notes

- Amazon Cognito User Pools: https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools.html
- Cognito groups: https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-user-groups.html
- IAM least privilege: https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html
- AWS Well-Architected Security Pillar: https://docs.aws.amazon.com/wellarchitected/latest/security-pillar/welcome.html
