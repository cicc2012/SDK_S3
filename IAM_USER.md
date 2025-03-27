# AWS IAM User Creation Demo: Implementing Least Privilege

## Overview

This guide demonstrates how to follow the **principle of least privilege** when creating IAM users in AWS. Instead of granting overly permissive access to services like **Amplify**, **Lambda**, and **API Gateway**, we'll create a refined IAM policy that provides only the minimal set of permissions necessary for specific tasks.

## Why Least Privilege Matters

Granting full access to AWS services exposes resources to:
- Accidental modification
- Misuse of permissions
- Increased security risks
- Compliance violations

## Creating a Least Privilege IAM Policy

Below is a refined IAM policy tailored to specific tasks for a web application that uses Amplify, Lambda, API Gateway, S3, and Textract.

Please note: if you would like to use AWS CDK as well. Please reference the [demo for CDK](https://github.com/cicc2012/IaC-demo), to configure the permissions needed.

### 1. AWS Amplify Permissions

Grant only the permissions needed to:
- **Manage** Amplify projects
- **Deploy** the app
- **Interact** with Amplify services like hosting and backend environments

```json
{
  "Effect": "Allow",
  "Action": [
    "amplify:CreateApp",
    "amplify:UpdateApp",
    "amplify:CreateBranch",
    "amplify:UpdateBranch",
    "amplify:CreateBackendEnvironment",
    "amplify:ListBackendEnvironments",
    "amplify:StartDeployment",
    "amplify:GetApp",
    "amplify:GetBranch",
    "amplify:GetBackendEnvironment",
    "amplify:ListApps",
    "amplify:ListBranches"
  ],
  "Resource": "*"
}
```

This allows the user to:
- Create and manage Amplify apps and branches
- Start deployments
- View Amplify environments and apps

### 2. Lambda Permissions

Grant only permissions to manage **Lambda functions** related to this project:

```json
{
  "Effect": "Allow",
  "Action": [
    "lambda:CreateFunction",
    "lambda:UpdateFunctionCode",
    "lambda:UpdateFunctionConfiguration",
    "lambda:InvokeFunction",
    "lambda:GetFunction",
    "lambda:ListFunctions"
  ],
  "Resource": "arn:aws:lambda:REGION:ACCOUNT_ID:function:YOUR_FUNCTION_NAME"
}
```

This policy restricts the permissions to:
- **Create and manage** a specific Lambda function
- **Invoke the function** as needed (e.g., when called by API Gateway)
- List available Lambda functions for proper management

### 3. API Gateway Permissions

Grant only the ability to **create** and **manage** the REST API resources:

```json
{
  "Effect": "Allow",
  "Action": [
    "apigateway:POST",
    "apigateway:PUT",
    "apigateway:GET",
    "apigateway:DELETE",
    "apigateway:PATCH"
  ],
  "Resource": "arn:aws:apigateway:REGION::/restapis/*"
}
```

This policy allows:
- **Creating** and managing API Gateway resources
- **Deploying** the API and viewing the API configuration

### 4. S3 Permissions

Grant only **update objects** permissions for a specific S3 bucket:

```json
{
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
	"s3:GetObjectAcl",
	"s3:GetObject",
    "s3:ListBucket",
    "s3:PutObjectVersionAcl",
    "s3:DeleteObject",
    "s3:PutObjectAcl"
  ],
  "Resource": [
    "arn:aws:s3:::your-bucket-name/*",
	"arn:aws:s3:::your-bucket-name"
  ]
}
```

This allows the user to:
- **Read** images from the specific S3 bucket without access to other S3 resources

### 5. Textract Permissions

Grant only permissions needed for text extraction:

```json
{
  "Effect": "Allow",
  "Action": [
    "textract:DetectDocumentText",
    "textract:StartDocumentTextDetection",
    "textract:GetDocumentTextDetection"
  ],
  "Resource": "*"
}
```

This allows the user to:
- **Call** Textract's operations for extracting text from images in S3

### 6. IAM Permissions for Managing Roles and Policies
We’ll grant permissions for creating and managing IAM roles and attaching policies. This is critical because the user needs to set the appropriate access policies for the Lambda functions and the Amplify app.

Here's how to structure the permissions for IAM:

```json
{
  "Effect": "Allow",
  "Action": [
    "iam:CreateRole",
    "iam:DeleteRole",
    "iam:AttachRolePolicy",
    "iam:DetachRolePolicy",
    "iam:PutRolePolicy",
    "iam:DeleteRolePolicy",
    "iam:ListRoles",
    "iam:GetRole"
  ],
  "Resource": "*"
}
```
- **iam:CreateRole**: Allows creating new IAM roles (e.g., for Lambda, Amplify).
- **iam:DeleteRole**: Allows deleting IAM roles when they are no longer needed.
- **iam:AttachRolePolicy** and **iam:DetachRolePolicy**: Allow attaching or detaching policies to IAM roles, such as giving Lambda functions the required permissions to access S3 and Textract.
- **iam:PutRolePolicy** and **iam:DeleteRolePolicy**: Allow adding and removing inline policies to IAM roles.
- **iam:ListRoles**: Allows listing IAM roles to see which roles are available to be modified.
- **iam:GetRole**: Allows retrieving the details of a role.
- **iam:PassRole**: This is important because Lambda functions or other services (like Amplify) will need the ability to assume these roles. Without iam:PassRole, they won't be able to pass the roles to services like Lambda or API Gateway.

### 7. Updated IAM Policy Including CloudFormation Permissions
```json
    {
      "Effect": "Allow",
      "Action": [
        "cloudformation:CreateStack",
        "cloudformation:UpdateStack",
        "cloudformation:DeleteStack",
        "cloudformation:DescribeStacks",
        "cloudformation:DescribeStackResources",
        "cloudformation:ListStacks",
        "cloudformation:ValidateTemplate",
        "cloudformation:ListStackResources",
        "cloudformation:CreateChangeSet",
        "cloudformation:ExecuteChangeSet"
      ],
      "Resource": "*"
    }
```
The user will need the following CloudFormation-specific permissions:

- **cloudformation:CreateStack**: Allows the user to create new CloudFormation stacks.
- **cloudformation:UpdateStack**: Allows the user to update existing stacks.
- **cloudformation:DeleteStack**: Allows the user to delete stacks.
- **cloudformation:DescribeStacks**: Allows the user to view the details of stacks.
- **cloudformation:DescribeStackResources**: Allows the user to view resources in a CloudFormation stack.
- **cloudformation:ListStacks**: Allows the user to list CloudFormation stacks.
- **cloudformation:ValidateTemplate**: Allows the user to validate CloudFormation templates before deployment.
- **cloudformation:ListStackResources**: Allows the user to list all resources in a stack.
iam:PassRole: Allows the user to pass IAM roles to CloudFormation when creating resources like Lambda functions, API Gateway, etc.
- **cloudformation:CreateChangeSet**, **cloudformation:ExecuteChangeSet**: Allows the user to manage change sets for stack updates.

## Complete IAM Policy

Here's the complete IAM policy combining all the above permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "amplify:CreateApp",
        "amplify:UpdateApp",
        "amplify:CreateBranch",
        "amplify:UpdateBranch",
        "amplify:CreateBackendEnvironment",
        "amplify:ListBackendEnvironments",
        "amplify:StartDeployment",
        "amplify:GetApp",
        "amplify:GetBranch",
        "amplify:GetBackendEnvironment",
        "amplify:ListApps",
        "amplify:ListBranches"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "lambda:CreateFunction",
        "lambda:UpdateFunctionCode",
        "lambda:UpdateFunctionConfiguration",
        "lambda:InvokeFunction",
        "lambda:GetFunction",
        "lambda:ListFunctions"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "apigateway:*"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
    	"s3:GetObjectAcl",
    	"s3:GetObject",
        "s3:ListBucket",
        "s3:PutObjectVersionAcl",
        "s3:DeleteObject",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "arn:aws:s3:::your-bucket-name/*",
    	"arn:aws:s3:::your-bucket-name"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "textract:DetectDocumentText",
        "textract:StartDocumentTextDetection",
        "textract:GetDocumentTextDetection"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole",
        "iam:DeleteRole",
        "iam:AttachRolePolicy",
        "iam:DetachRolePolicy",
        "iam:PutRolePolicy",
        "iam:DeleteRolePolicy",
        "iam:ListRoles",
        "iam:GetRole"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudformation:CreateStack",
        "cloudformation:UpdateStack",
        "cloudformation:DeleteStack",
        "cloudformation:DescribeStacks",
        "cloudformation:DescribeStackResources",
        "cloudformation:ListStacks",
        "cloudformation:ValidateTemplate",
        "cloudformation:ListStackResources",
        "cloudformation:CreateChangeSet",
        "cloudformation:ExecuteChangeSet"
      ],
      "Resource": "*"
    }
  ]
}
```

## Implementation Steps

1. Sign in to the AWS Management Console
2. Navigate to IAM service
3. Click "Users" and then "Add user"
4. Enter a username and select "Programmatic access"
5. Click "Next: Permissions"
6. Choose "Attach existing policies directly"
7. Click "Create policy"
8. In the policy editor, select the JSON tab
9. Paste the complete policy JSON above (replace placeholders with your values)
10. Click "Review policy"
11. Name your policy (e.g., "AppDeploymentLeastPrivilegePolicy")
12. Click "Create policy"
13. Return to the user creation screen, refresh the policy list
14. Find and select your newly created policy
15. Complete the user creation process

## Conclusion

This IAM policy implements the **principle of least privilege** by ensuring the user can only perform specific actions necessary for their role. By limiting access to only required resources and actions, this approach:

- Reduces security risks
- Ensures minimal permissions needed to complete tasks
- Protects against accidental changes
- Meets compliance requirements for access control

Remember to update these permissions as your application requirements change, and regularly review them to maintain the principle of least privilege.
