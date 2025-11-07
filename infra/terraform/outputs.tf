output "role_arn" {
  description = "ARN of the IAM role for Resonant (copy this to Resonant app)"
  value       = module.iam.role_arn
}

output "role_name" {
  description = "Name of the IAM role"
  value       = module.iam.role_name
}

output "external_id" {
  description = "External ID used for this role"
  value       = var.external_id
  sensitive   = true
}

output "policy_arn" {
  description = "ARN of the managed policy"
  value       = module.iam.policy_arn
}

output "aws_account_id" {
  description = "Your AWS Account ID"
  value       = data.aws_caller_identity.current.account_id
}