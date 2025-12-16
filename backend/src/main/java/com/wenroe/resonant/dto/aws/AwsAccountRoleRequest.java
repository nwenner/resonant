package com.wenroe.resonant.dto.aws;

import lombok.Data;

@Data
public class AwsAccountRoleRequest {

  private String accountAlias;
  private String roleArn;
  private String externalId;
}