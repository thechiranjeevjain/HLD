terraform { required_providers { aws = { source = "hashicorp/aws", version = "~> 6.0" } } }
provider "aws" { region = var.region }
variable "region" { default = "ap-south-1" }
resource "aws_ecr_repository" "app" { name = "ai-risk-fraud-assistant" image_scanning_configuration { scan_on_push = true } }
# Interview deployment boundary: use well-reviewed VPC/EKS/RDS/ElastiCache/MSK modules in a real account.
# This starter intentionally avoids pretending that a few insecure resources form a production AWS platform.
output "ecr_repository_url" { value = aws_ecr_repository.app.repository_url }
