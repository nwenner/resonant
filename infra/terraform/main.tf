terraform {
  backend "s3" {
    bucket = "s3StateBucket-${var.stage}"
    key    = "path/to/my/key"
    region = "us-east-2"
  }
}

provider "aws" {
  region = "us-east-2"
  // TBD additional implementation
}

data "aws_caller_identity" "current" {}

module "iam" {
  source = "modules/iam"

  external_id             = var.external_id
  include_ecs             = var.include_ecs
  include_lambda          = var.include_lambda
  resonant_aws_account_id = var.resonant_aws_account_id
  role_name               = var.role_name
}
