output "database_endpoint" { value=aws_db_instance.orders.address }
output "redis_endpoint" { value=aws_elasticache_replication_group.orders.primary_endpoint_address }
output "kafka_bootstrap_brokers" { value=aws_msk_serverless_cluster.events.bootstrap_brokers_sasl_iam }
output "ecr_repository_url" { value=aws_ecr_repository.app.repository_url }
output "database_password" { value=random_password.db.result sensitive=true }
