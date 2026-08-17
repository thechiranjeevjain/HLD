# Terraform Infrastructure

This Terraform stack is the infrastructure explanation for the Employee Document Upload System.

It creates:

- VPC with public, private app, and private data subnets.
- Internet-facing ALB.
- ECS Fargate service for the Spring Boot API.
- Private RDS PostgreSQL database.
- Private KMS-encrypted S3 document bucket.
- Cognito user pool, web client, and RBAC groups.
- IAM task roles scoped to S3, KMS, and Secrets Manager.
- CloudWatch logs and alarms.
- AWS Backup plan and vault for RDS recovery points.

## Commands

```powershell
terraform init
terraform fmt -recursive
terraform validate
terraform plan -var-file terraform.tfvars
```

The local workstation used to create this project did not have Terraform installed, so run validation on a machine with Terraform before applying.

## Production Hardening To Add

- Remote Terraform backend with encrypted S3 state and DynamoDB locking.
- Route 53 DNS record and friendly domain name.
- AWS WAF on the public load balancer.
- VPC endpoints for S3, ECR, CloudWatch Logs, Secrets Manager, and KMS.
- Organization-level CloudTrail and AWS Config.
- Separate AWS accounts for dev, stage, and prod.
- Production approval gate before `terraform apply`.
