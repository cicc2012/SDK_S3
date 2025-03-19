package edu.uco;

import java.nio.file.Paths;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.AddPermissionRequest;
import software.amazon.awssdk.services.lambda.model.AddPermissionResponse;
// import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
// import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.s3.S3Client;
// import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.Event;
import software.amazon.awssdk.services.s3.model.FilterRule;
import software.amazon.awssdk.services.s3.model.FilterRuleName;
import software.amazon.awssdk.services.s3.model.LambdaFunctionConfiguration;
// import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
// import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.NotificationConfigurationFilter;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
// import software.amazon.awssdk.services.s3.model.NotificationConfigurationFilter;
import software.amazon.awssdk.services.s3.model.S3KeyFilter;
// import software.amazon.awssdk.services.s3.model.FilterRule;
// import software.amazon.awssdk.services.s3.model.FilterRuleName;


public class CloudFormationDemo {
    private static final Logger logger = LoggerFactory.getLogger(CloudFormationDemo.class);
    
    private void deployCloudFormationTemplate(Region region, String url, String stackName) {
        // Initialize CloudFormation Client
        CloudFormationClient cfClient = CloudFormationClient.builder()
                .region(region)
                // .credentialsProvider(ProfileCredentialsProvider.create())
                .build();


        // Define parameters for the stack (if needed)
        // Parameter parameter = Parameter.builder()
        //         .parameterKey("ParameterKey") // Replace with actual key
        //         .parameterValue("ParameterValue") // Replace with actual value
        //         .build();

        // Create the CreateStackRequest
        CreateStackRequest createStackRequest = CreateStackRequest.builder()
                .stackName(stackName)
                .templateURL(url)  // S3 URL of the template
                // .parameters(parameter)   // We have default values in the template, so no need to pass parameters
                .capabilities(Capability.CAPABILITY_IAM)  // Include if you're creating IAM resources
                .build();

        try {
            // Create the stack
            CreateStackResponse createStackResponse = cfClient.createStack(createStackRequest);

            // Check if the stack creation was successful
            String stackId = createStackResponse.stackId();
            System.out.println("Stack creation initiated. Stack ID: " + stackId);
        } catch (CloudFormationException e) {
            System.err.println("Error creating stack: " + e.getMessage());
        }

        // Optionally, check the status of the stack creation
        // (this is a basic check; you may want to implement polling for real production use)
        boolean isStackCreated = false;
        while (!isStackCreated) {
            try {
                // Wait a bit and then check the status of the stack
                Thread.sleep(5000);

                // Fetch the stack details (could use describeStacks or any other relevant method)
                var stackStatus = cfClient.describeStacks(builder -> builder.stackName(stackName))
                        .stacks()
                        .get(0)
                        .stackStatus();

                // Check the status
                if (stackStatus.equals(StackStatus.CREATE_COMPLETE)) {
                    System.out.println("Stack created successfully!");
                    isStackCreated = true;
                } else if (stackStatus.equals(StackStatus.CREATE_FAILED)) {
                    System.out.println("Stack creation failed.");
                    isStackCreated = true;
                } else {
                    System.out.println("Stack is in progress... Status: " + stackStatus);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Stack stack = cfClient.describeStacks(builder -> builder.stackName(stackName))
            .stacks()
            .get(0);

        for (Output output : stack.outputs()) {
            System.out.println("  Output Key: " + output.outputKey());
            System.out.println("  Output Value: " + output.outputValue());
        }

        // Close the CloudFormation client
        cfClient.close();
    }

    
    private void setEventNotification(String accountId, Region region, String bucketName, String lambdaFunctionName, String functionArn, String amplifyAppId) {
        try {
            // Add permission to allow S3 to invoke the Lambda function
            LambdaClient lambdaClient = LambdaClient.builder()
                    .region(region)
                    // .credentialsProvider(ProfileCredentialsProvider.create())
                    .build();
            String sourceArn = "arn:aws:s3:::" + bucketName;
            AddPermissionRequest permissionRequest = AddPermissionRequest.builder()
                    .functionName(lambdaFunctionName)
                    .principal("s3.amazonaws.com")
                    .statementId("AllowS3EventInvoke")  // A unique ID for the permission statement
                    .action("lambda:InvokeFunction")
                    .sourceArn(sourceArn)
                    .build();

            AddPermissionResponse permissionResponse = lambdaClient.addPermission(permissionRequest);
            System.out.println("Permission added: " + permissionResponse);
        } catch (Exception e) {
            logger.error("Failed to give permission to s3 to trigger the lambda function: {}", e.getMessage());
        }

        // Build the bucket policy with the dynamic amplifyAppId
        String policyJson = String.format("{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Sid\": \"AllowAmplifyToListPrefix_%s_dev_proj3_index_zip\",\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": {\n" +
                "        \"Service\": \"amplify.amazonaws.com\"\n" +
                "      },\n" +
                "      \"Action\": \"s3:ListBucket\",\n" +
                "      \"Resource\": \"arn:aws:s3:::%s\",\n" +
                "      \"Condition\": {\n" +
                "        \"StringEquals\": {\n" +
                "          \"aws:SourceAccount\": \"%s\",\n" +
                "          \"s3:prefix\": \"proj3/index.zip\",\n" +
                "          \"aws:SourceArn\": \"arn:aws:amplify:us-east-1:%s:apps/%s/branches/dev\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "},\n" +
                "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Sid\": \"AllowAmplifyToReadPrefix_%s_dev_proj3_index_zip\",\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": {\n" +
                "        \"Service\": \"amplify.amazonaws.com\"\n" +
                "      },\n" +
                "      \"Action\": \"s3:GetObject\",\n" +
                "      \"Resource\": \"arn:aws:s3:::%s\",\n" +
                "      \"Condition\": {\n" +
                "        \"StringEquals\": {\n" +
                "          \"aws:SourceAccount\": \"%s\",\n" +
                "          \"aws:SourceArn\": \"arn:aws:amplify:us-east-1:%s:apps/%s/branches/dev\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "},\n" +
                "{\n" +
		        "	\"Effect\": \"Deny\",\n" +
		        "	\"Principal\": \"*\",\n" +
		        "	\"Action\": \"s3:*\",\n" +
		        "	\"Resource\": \"arn:aws:s3:::%s/*\",\n" +
		        "	\"Condition\": {\n" +
		        "		\"Bool\": {\n" +
		        "			\"aws:SecureTransport\": \"false\"\n" +
		        "		}\n" +
		        "	}\n" +
                "}", amplifyAppId, bucketName, accountId, accountId,amplifyAppId,
                 amplifyAppId, bucketName, accountId, accountId, amplifyAppId, bucketName);

        S3Client s3Client = S3Client.builder()
                .region(region)
                // .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
        // LambdaClient lambdaClient = LambdaClient.builder()
        //         .region(region)
        //         .credentialsProvider(ProfileCredentialsProvider.create())
        //         .build();

        // Create a request to get function details
        // GetFunctionRequest request = GetFunctionRequest.builder()
        //             .functionName(lambdaFunctionName)
        //             .build();
        
        try {
            // Call the getFunction API
            // GetFunctionResponse response = lambdaClient.getFunction(request);

            // Get the function ARN
            // String functionArn = response.configuration().functionArn();
            
            // Configure event notification with filters
            FilterRule prefixRule = FilterRule.builder()
                    .name(FilterRuleName.PREFIX)
                    .value("proj3/index.zip")
                    .build();


            S3KeyFilter s3KeyFilter = S3KeyFilter.builder()
                    .filterRules(prefixRule)
                    .build();

            NotificationConfigurationFilter notificationFilter = NotificationConfigurationFilter.builder()
                    .key(s3KeyFilter)
                    .build();

            LambdaFunctionConfiguration lambdaConfig = LambdaFunctionConfiguration.builder()
                    .lambdaFunctionArn(functionArn)
                    .events(Event.S3_OBJECT_CREATED)
                    .filter(notificationFilter)
                    .build();

            NotificationConfiguration notificationConfig = NotificationConfiguration.builder()
                    .lambdaFunctionConfigurations(lambdaConfig)
                    .build();

            PutBucketNotificationConfigurationRequest notificationRequest = PutBucketNotificationConfigurationRequest.builder()
                    .bucket(bucketName)
                    .notificationConfiguration(notificationConfig)
                    .build();
            
            PutBucketPolicyRequest policyRequest = PutBucketPolicyRequest.builder()
                    .bucket(bucketName)
                    .policy(policyJson)
                    .build();

            s3Client.putBucketNotificationConfiguration(notificationRequest);
            s3Client.putBucketPolicy(policyRequest);

            // Grant S3 permission to invoke Lambda
            // String sourceAccount = accountId;
            // String sourceArn = "arn:aws:s3:::" + bucketName;

            // AddPermissionRequest permissionRequest = AddPermissionRequest.builder()
            //         .functionName(lambdaFunctionName)
            //         .statementId("s3-invoke-function")
            //         .action("lambda:InvokeFunction")
            //         .principal("s3.amazonaws.com")
            //         .sourceAccount(sourceAccount)
            //         .sourceArn(sourceArn)
            //         .build();

            // lambdaClient.addPermission(permissionRequest);

            logger.info("S3 event notification set up successfully!");
            logger.info("Function ARN: {}", functionArn);

        } catch (Exception e) {
            logger.error("Error setting up S3 event notification: {}", e.getMessage());
        }
        
    }

    private void uploadS3File(String bucketName, String keyName, String filePath, Region region) {       
        try {
            // Create an S3 client
            S3Client s3Client = S3Client.builder()
            .region(region)
            // .credentialsProvider(ProfileCredentialsProvider.create())
            .build();

            // Create a PutObjectRequest to upload the file
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)        // Specify the bucket name
                    .key(keyName)              // Specify the S3 object key (file name in the bucket)
                    .build();

            // Upload the file
            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromFile(Paths.get(filePath)) // Specify the local file to upload
            );

            // Print out the response metadata
            System.out.println("Upload complete. Response: " + response);
        } catch (Exception e) {
            System.err.println("Failed to upload file: " + e.getMessage());
            logger.error("Failed to upload file: ", e.getMessage());
        }
    }

    public static void main(String[] args) {

        
        CloudFormationDemo demo = new CloudFormationDemo();
        
        // AWS Region (choose the correct region)
        Region region = Region.US_EAST_1;
        String bucketName = "uco-cicc-media";
        String accoundId = "590183882567";

        // Define S3 URL of the CloudFormation template
        String backendUrl = "https://uco-cicc-media.s3.us-east-1.amazonaws.com/proj3/proj3_backend.yml";
        String frontendUrl = "https://uco-cicc-media.s3.us-east-1.amazonaws.com/proj3/proj3_frontkend.yml";
        String amplifyUrl = "https://uco-cicc-media.s3.us-east-1.amazonaws.com/proj3/proj3_amplify.yml";
        
        // Define the stack name
        String backendStackName = "cicc-participation-backend";
        String frontendStackName = "cicc-participation-frontend";
        String amplifyStackName = "cicc-participation-amplify";

        Scanner scanner = new Scanner(System.in);
        System.out.println("*****************   STEP 1 : backend ***********************");
        demo.deployCloudFormationTemplate(region, backendUrl, backendStackName); 

        System.out.println("*****************   STEP 2 : frontend ***********************");
        System.out.println("You need to process the output values from the backend stack: ");
        System.out.println("Please enter \"Yes\" when you are ready to deploy the frontend stack: ");
        String response = scanner.nextLine();
        if (!response.equalsIgnoreCase("Yes")) {
            System.out.println("Exiting the program...");
            System.exit(0);
        }
        else {
            demo.deployCloudFormationTemplate(region, frontendUrl, frontendStackName);
        }

        System.out.println("*****************   STEP 3 : final deployment ***********************");
        System.out.println("You need to process the output values from the frontend stack: ");
        System.out.println("Please enter \"Yes\" when you are ready to deploy the amplify stack: ");
        response = scanner.nextLine();
        if (!response.equalsIgnoreCase("Yes")) {
            System.out.println("Exiting the program...");
            System.exit(0);
        }
        else {
            demo.deployCloudFormationTemplate(region, amplifyUrl, amplifyStackName);
        }

        // set event notification
        System.out.println("*****************   STEP 4 : event setting ***********************");
        System.out.println("You need to process the output values from the amplify stack: ");
        System.out.println("Please enter the name of the lambda function to deploy the web app in amplify, when you are ready to deploy the amplify stack: ");

        String lambdaFunctionName = scanner.nextLine();

        System.out.println("Please enter the ARN of the lambda function to deploy the web app in amplify, when you are ready to deploy the amplify stack: ");
        String functionArn = scanner.nextLine();

        System.out.println("Please enter the app Id of the amplify app, when you are ready to deploy the amplify stack: ");
        String amplifyAppId = scanner.nextLine();
        demo.setEventNotification(accoundId, region, bucketName, lambdaFunctionName, functionArn, amplifyAppId);
        
        System.out.println("*****************   STEP 5 : test ***********************");
        System.out.println("Please enter local path for the file to be uploaded to s3 bucket: ");
        String filePath = scanner.nextLine(); // Replace with your local file path
        System.out.println("Please enter key for this file in your s3 bucket: ");
        String keyName = scanner.nextLine(); // The key (name) you want for the file in S3
        demo.uploadS3File(bucketName, keyName, filePath, region);
        scanner.close();

        /* 
        try (S3Client s3Client = S3Client.create()) {

            // List all buckets in your account
            ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
            ListBucketsResponse response = s3Client.listBuckets(listBucketsRequest);

            // Print the bucket names
            System.out.println("S3 Buckets:");
            for (Bucket bucket : response.buckets()) {
                System.out.println(bucket.name());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}