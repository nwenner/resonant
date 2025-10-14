terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
  }
}

provider "aws" {
  # Configure via environment variables or AWS CLI profile
}

# Data source to get current AWS account ID
data "aws_caller_identity" "current" {}

# IAM Policy for read-only access
resource "aws_iam_policy" "resonant_readonly" {
  name        = "${var.role_name}-Policy"
  description = "Read-only permissions for Resonant to scan AWS resources and tags"
  path        = "/"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [
        {
          Sid    = "EC2ReadOnly"
          Effect = "Allow"
          Action = [
            "ec2:DescribeInstances",
            "ec2:DescribeVolumes",
            "ec2:DescribeSnapshots",
            "ec2:DescribeImages",
            "ec2:DescribeSecurityGroups",
            "ec2:DescribeVpcs",
            "ec2:DescribeSubnets",
            "ec2:DescribeNetworkInterfaces",
            "ec2:DescribeTags",
            "ec2:DescribeRegions",
            "ec2:DescribeAvailabilityZones"
          ]
          Resource = "*"
        },
        {
          Sid    = "S3ReadOnly"
          Effect = "Allow"
          Action = [
            "s3:ListAllMyBuckets",
            "s3:ListBucket",
            "s3:GetBucketLocation",
            "s3:GetBucketTagging",
            "s3:GetBucketVersioning",
            "s3:GetBucketEncryption",
            "s3:GetBucketPublicAccessBlock",
            "s3:GetBucketLogging",
            "s3:GetBucketPolicy"
          ]
          Resource = "*"
        },
        {
          Sid    = "RDSReadOnly"
          Effect = "Allow"
          Action = [
            "rds:DescribeDBInstances",
            "rds:DescribeDBClusters",
            "rds:DescribeDBSnapshots",
            "rds:DescribeDBClusterSnapshots",
            "rds:ListTagsForResource"
          ]
          Resource = "*"
        },
        {
          Sid    = "ElastiCacheReadOnly"
          Effect = "Allow"
          Action = [
            "elasticache:DescribeCacheClusters",
            "elasticache:DescribeReplicationGroups",
            "elasticache:ListTagsForResource"
          ]
          Resource = "*"
        },
        {
          Sid    = "DynamoDBReadOnly"
          Effect = "Allow"
          Action = [
            "dynamodb:ListTables",
            "dynamodb:DescribeTable",
            "dynamodb:ListTagsOfResource"
          ]
          Resource = "*"
        },
        {
          Sid    = "CloudFrontReadOnly"
          Effect = "Allow"
          Action = [
            "cloudfront:ListDistributions",
            "cloudfront:GetDistribution",
            "cloudfront:ListTagsForResource"
          ]
          Resource = "*"
        }
      ],
        var.include_lambda ? [{
        Sid    = "LambdaReadOnly"
        Effect = "Allow"
        Action = [
          "lambda:ListFunctions",
          "lambda:GetFunction",
          "lambda:GetFunctionConfiguration",
          "lambda:ListTags"
        ]
        Resource = "*"
      }] : [],
        var.include_ecs ? [{
        Sid    = "ECSReadOnly"
        Effect = "Allow"
        Action = [
          "ecs:ListClusters",
          "ecs:ListServices",
          "ecs:ListTasks",
          "ecs:DescribeClusters",
          "ecs:DescribeServices",
          "ecs:DescribeTasks",
          "ecs:ListTagsForResource"
        ]
        Resource = "*"
      }] : []
    )
  })

  tags = {
    ManagedBy = "Resonant"
    Purpose   = "TagComplianceScanning"
  }
}

# IAM Role for cross-account access
resource "aws_iam_role" "resonant" {
  name               = var.role_name
  description        = "Allows Resonant to scan AWS resources for tag compliance"
  max_session_duration = 3600  # 1 hour

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${var.resonant_aws_account_id}:root"
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "sts:ExternalId" = var.external_id
          }
        }
      }
    ]
  })

  tags = {
    ManagedBy = "Resonant"
    Purpose   = "TagComplianceScanning"
  }
}

# Attach policy to role
resource "aws_iam_role_policy_attachment" "resonant_readonly" {
  role       = aws_iam_role.resonant.name
  policy_arn = aws_iam_policy.resonant_readonly.arn
}