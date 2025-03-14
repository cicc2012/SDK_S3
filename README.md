# **Demo: Create S3 Event Notification for Object Creation Using AWS CDK**

This demo walks you through creating an S3 event notification for an existing private S3 bucket using AWS CDK. The notification triggers a Lambda function when an object is created in the bucket.

---

## **Prerequisites**

Ensure the following are installed on your Windows 10 machine:

- **AWS CLI**: [Install AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html) and configure it (`aws configure`).
- **Node.js**: [Download Node.js](https://nodejs.org/).
- **AWS CDK**: Install globally via npm:

```bash
npm install -g aws-cdk
```

- **Python** (if using Python for Lambda): Install from [Python.org](https://www.python.org/downloads/).

---

## **Steps**

### **Step 1: Initialize a CDK Project**

1. Open a terminal (PowerShell or Command Prompt).
2. Create a new directory and initialize the CDK project:

```bash
mkdir s3-event-notification
cd s3-event-notification
cdk init app --language=typescript
```


---

### **Step 2: Install Required CDK Modules**

Install the necessary AWS CDK libraries for S3, Lambda, and S3 notifications:

```bash
npm install @aws-cdk/aws-s3 @aws-cdk/aws-lambda @aws-cdk/aws-s3-notifications
```

---

### **Step 3: Modify the Stack Code**

Edit the `lib/s3-event-notification-stack.ts` file to define your S3 bucket, Lambda function, and event notification.

```typescript
import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as s3n from 'aws-cdk-lib/aws-s3-notifications';

export class S3EventNotificationStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Import an existing S3 bucket by its name
    const existingBucket = s3.Bucket.fromBucketName(this, 'ExistingBucket', 'your-existing-bucket-name');

    // Create a Lambda function that will be triggered by the event
    const lambdaFunction = new lambda.Function(this, 'MyLambdaFunction', {
      runtime: lambda.Runtime.PYTHON_3_9,
      handler: 'index.handler',
      code: lambda.Code.fromInline(`
        def handler(event, context):
            print("Event received:", event)
      `),
    });

    // Add an event notification for object creation
    existingBucket.addEventNotification(
      s3.EventType.OBJECT_CREATED,
      new s3n.LambdaDestination(lambdaFunction)
    );

    // Grant the bucket permission to invoke the Lambda function
    existingBucket.grantRead(lambdaFunction);
  }
}
```

Replace `your-existing-bucket-name` with the name of your private S3 bucket.

---

### **Step 4: Deploy the Stack**

1. Bootstrap your environment (if not already done):

```bash
cdk bootstrap
```

2. Deploy the stack:

```bash
cdk deploy
```


---

### **Step 5: Test the Notification**

1. Upload a file to your S3 bucket using AWS CLI:

```bash
aws s3 cp test-file.txt s3://your-existing-bucket-name/
```

2. Check CloudWatch Logs for your Lambda function:
    - Go to **CloudWatch > Logs > Log Groups** in the AWS Management Console.
    - Find and view logs for your Lambda function.

---

## **Optional Enhancements**

### **Using AWS SDK for Notification Configuration**

If you'd prefer to use AWS SDK directly (e.g., Python's `boto3`), you can configure notifications programmatically:

```python
import boto3

s3 = boto3.client('s3')

response = s3.put_bucket_notification_configuration(
    Bucket='your-existing-bucket-name',
    NotificationConfiguration={
        'LambdaFunctionConfigurations': [
            {
                'LambdaFunctionArn': 'arn:aws:lambda:region:account-id:function:function-name',
                'Events': ['s3:ObjectCreated:*'],
            },
        ]
    }
)

print("Notification configured:", response)
```

Ensure proper IAM permissions for both S3 and Lambda.

---

## **Key Considerations**

1. **IAM Permissions**:
    - Ensure your IAM role has permissions to manage S3 buckets, Lambda functions, and event notifications.
2. **Private Buckets**:
    - Since your bucket is private, ensure proper IAM policies are in place.
3. **Testing Locally**:
    - Use tools like AWS SAM CLI or `localstack` for local testing of Lambda functions triggered by S3 events.

---

## **Summary**

By following this demo:

- You’ve created an event notification on an existing private S3 bucket using AWS CDK.
- A Lambda function is triggered when an object is created in the bucket.
- You’ve tested the setup by uploading files and viewing logs.

This approach combines Infrastructure as Code (CDK) with serverless architecture principles to create scalable and maintainable solutions!