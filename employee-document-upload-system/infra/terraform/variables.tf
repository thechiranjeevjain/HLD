variable "aws_region" {
  description = "AWS region for the workload."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Short name used in resource names."
  type        = string
  default     = "employee-docs"
}

variable "environment" {
  description = "Environment name, for example dev, stage, or prod."
  type        = string
  default     = "dev"
}

variable "vpc_cidr" {
  description = "CIDR block for the workload VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to reach the public load balancer."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "container_image" {
  description = "API container image. Leave empty to use this stack's ECR repository with the latest tag."
  type        = string
  default     = ""
}

variable "app_port" {
  description = "Container port exposed by the API."
  type        = number
  default     = 8080
}

variable "api_cpu" {
  description = "Fargate task CPU units."
  type        = number
  default     = 512
}

variable "api_memory" {
  description = "Fargate task memory in MiB."
  type        = number
  default     = 1024
}

variable "api_desired_count" {
  description = "Number of API tasks to run."
  type        = number
  default     = 1
}

variable "database_name" {
  description = "PostgreSQL database name."
  type        = string
  default     = "employeedocs"
}

variable "database_username" {
  description = "Application database username."
  type        = string
  default     = "employee_docs_app"
}

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Initial RDS storage in GiB."
  type        = number
  default     = 20
}

variable "postgres_engine_version" {
  description = "Pinned PostgreSQL major version."
  type        = string
  default     = "16"
}

variable "enable_multi_az" {
  description = "Enable Multi-AZ RDS deployment. Usually false in dev, true in prod."
  type        = bool
  default     = false
}

variable "backup_retention_days" {
  description = "RDS automated backup retention."
  type        = number
  default     = 7
}

variable "cloudwatch_log_retention_days" {
  description = "CloudWatch log retention in days."
  type        = number
  default     = 14
}

variable "alarm_email" {
  description = "Optional email address for CloudWatch alarm notifications."
  type        = string
  default     = ""
}

variable "certificate_arn" {
  description = "Optional ACM certificate ARN. When set, the ALB serves HTTPS and redirects HTTP to HTTPS."
  type        = string
  default     = ""
}

variable "callback_urls" {
  description = "Allowed Cognito app client callback URLs."
  type        = list(string)
  default     = ["http://localhost:3000/callback"]
}

variable "logout_urls" {
  description = "Allowed Cognito app client logout URLs."
  type        = list(string)
  default     = ["http://localhost:3000/logout"]
}

variable "cognito_groups" {
  description = "Application RBAC groups created in Cognito."
  type        = list(string)
  default     = ["EMPLOYEE", "HR_REVIEWER", "AUDITOR", "ADMIN"]
}
