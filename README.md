
## **Demo: Create S3 Event Notification Using AWS SDK for Java**

This demo guides you through setting up an S3 event notification for an existing bucket and linking it to an existing Lambda function using the AWS SDK for Java.

Please **note**: the [provided program](CloudFormationDemo.java) includes the entire stack, not just for seting up the bucket object event. If you only need to set up the event notification, you can comment / revmoe other code in the main function.

### **Prerequisites**

1. **AWS SDK for Java**: Ensure you have the AWS SDK for Java (version 2.x) installed.
2. **Existing S3 Bucket**: Have an existing S3 bucket.
3. **Existing Lambda Function**: Have an existing Lambda function.
4. **AWS Credentials**: Ensure your AWS credentials are properly configured.

### **Step 0: Java Extension in VS Code**
You can install Extension Pack for Java in VS Code, to set up the build tool.
In the welcome page after the installation of this extension, you can also install a JDK.

#### AWS credentials
Install AWS CLI, if you haven't. Here is a link for [download and instruction](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html).

You can use the credentials of your AWS root user, or a new IAM user (you can reference [IAM_USER](IAM_USER.md)). **Please note: if you would like to use AWS CDK as well. Please reference the [demo for CDK](https://github.com/cicc2012/IaC-demo), to configure the permissions needed.**

You can download the access key of your user: you can find the *Security credentials* panel under this user. Then click *Create Access Key* to create one: make sure you can see the *access key id* and *secret access key*.
Once this is done, you can open a terminal, and use the following command to set up your account.
```bash
aws configure
```
In the terminal, you can paste the *access key id* and *secret access key*.

**Please keep your access key protected,e.g., do not sync it to cloud storage, do not save it as plain text, etc.**

### **Step 1: Set Up the Project**

1. Create a new Java project in your IDE, e.g., VS Code.
2. Add the AWS SDK for Java dependency to your `pom.xml` if using Maven:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.26</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>lambda</artifactId>
    <version>2.20.26</version>
</dependency>
```
You can use the example [pom.xml](pom.xml).

### **Step 2: Create the Java Program**

Create a new Java Program to set the event notification for you S3 bucket. You can find this in method 
`private void setEventNotification(String accountId, Region region, String bucketName, String lambdaFunctionName, String functionArn, String amplifyAppId)`
in the [given program](CloudFormationDemo.java).

Here is part of the sample code (you need to make necessary changes based on your account information):

```java
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.AddPermissionRequest;

public class S3EventNotificationSetup {
    public static void main(String[] args) {
        String bucketName = "your-existing-bucket-name";
        String lambdaFunctionArn = "arn:aws:lambda:region:account-id:function:your-function-name";
        String lambdaFunctionName = "your-function-name";

        S3Client s3Client = S3Client.builder().build();
        LambdaClient lambdaClient = LambdaClient.builder().build();

        // Configure event notification
        LambdaFunctionConfiguration lambdaConfig = LambdaFunctionConfiguration.builder()
                .lambdaFunctionArn(lambdaFunctionArn)
                .events(Event.S3_OBJECT_CREATED_PUT)
                .build();

        NotificationConfiguration notificationConfig = NotificationConfiguration.builder()
                .lambdaFunctionConfigurations(lambdaConfig)
                .build();

        PutBucketNotificationConfigurationRequest request = PutBucketNotificationConfigurationRequest.builder()
                .bucket(bucketName)
                .notificationConfiguration(notificationConfig)
                .build();

        s3Client.putBucketNotificationConfiguration(request);

        // Grant S3 permission to invoke Lambda
        String sourceAccount = "your-account-id";
        String sourceArn = "arn:aws:s3:::" + bucketName;

        AddPermissionRequest permissionRequest = AddPermissionRequest.builder()
                .functionName(lambdaFunctionName)
                .statementId("s3-invoke-function")
                .action("lambda:InvokeFunction")
                .principal("s3.amazonaws.com")
                .sourceAccount(sourceAccount)
                .sourceArn(sourceArn)
                .build();

        lambdaClient.addPermission(permissionRequest);

        System.out.println("S3 event notification set up successfully!");
    }
}
```


### **Step 3: Run the Code**

1. Replace the placeholder values in the code:
    - `your-existing-bucket-name`
    - `your-function-name`
    - `region`
    - `account-id`
    - `your-account-id`
2. Run the Java class to set up the event notification.

### **Step 4: Test the Setup**

1. Upload a file to your S3 bucket using the AWS CLI or Console:

```bash
aws s3 cp index.zip s3://your-existing-bucket-name/your-path
```

2. Check the CloudWatch logs for your Lambda function to verify it was triggered.

