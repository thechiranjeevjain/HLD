output "api_url" {
  description = "Public load balancer URL for the API."
  value       = "${var.certificate_arn == "" ? "http" : "https"}://${aws_lb.api.dns_name}"
}

output "document_bucket_name" {
  description = "Private S3 bucket used for employee documents."
  value       = aws_s3_bucket.documents.bucket
}

output "database_endpoint" {
  description = "RDS endpoint. Keep private; exposed here for operators."
  value       = aws_db_instance.main.address
}

output "cognito_user_pool_id" {
  description = "Cognito user pool id."
  value       = aws_cognito_user_pool.main.id
}

output "cognito_user_pool_client_id" {
  description = "Cognito web app client id."
  value       = aws_cognito_user_pool_client.web.id
}

output "ecr_repository_url" {
  description = "ECR repository where CI can push the API image."
  value       = aws_ecr_repository.api.repository_url
}
