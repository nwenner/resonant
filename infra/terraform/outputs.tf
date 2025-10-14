output "role_arn" {
  description = "ARN of the IAM role for Resonant (copy this to Resonant app)"
  value       = aws_iam_role.resonant.arn
}

output "role_name" {
  description = "Name of the IAM role"
  value       = aws_iam_role.resonant.name
}

output "external_id" {
  description = "External ID used for this role"
  value       = var.external_id
  sensitive   = true
}

output "policy_arn" {
  description = "ARN of the managed policy"
  value       = aws_iam_policy.resonant_readonly.arn
}

output "aws_account_id" {
  description = "Your AWS Account ID"
  value       = data.aws_caller_identity.current.account_id
}