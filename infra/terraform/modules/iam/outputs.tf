output "role_arn" {
  value = aws_iam_role.resonant.arn
}

output "role_name" {
  value = aws_iam_role.resonant.name
}

output "policy_arn" {
  value = aws_iam_policy.resonant_readonly.arn
}


