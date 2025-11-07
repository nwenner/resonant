export const CLOUDFORMATION_TEMPLATE = `AWSTemplateFormatVersion: '2010-09-09'
Description: 'Resonant AWS Account Integration Role'

Parameters:
  ExternalId:
    Type: String
    Description: External ID provided by Resonant
    NoEcho: true

Resources:
  ResonantRole:
    Type: AWS::IAM::Role
    Properties:
      RoleName: ResonantComplianceRole
      AssumeRolePolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Effect: Allow
            Principal:
              AWS: 'arn:aws:iam::YOUR_RESONANT_ACCOUNT:root'
            Action: 'sts:AssumeRole'
            Condition:
              StringEquals:
                'sts:ExternalId': !Ref ExternalId
      ManagedPolicyArns:
        - 'arn:aws:iam::aws:policy/ReadOnlyAccess'
      Policies:
        - PolicyName: ResonantTaggingPolicy
          PolicyDocument:
            Version: '2012-10-17'
            Statement:
              - Effect: Allow
                Action:
                  - 'tag:GetResources'
                  - 'tag:GetTagKeys'
                  - 'tag:GetTagValues'
                  - 'resourcegroupstaggingapi:*'
                Resource: '*'

Outputs:
  RoleArn:
    Description: ARN of the created IAM role
    Value: !GetAtt ResonantRole.Arn
    Export:
      Name: ResonantRoleArn`;