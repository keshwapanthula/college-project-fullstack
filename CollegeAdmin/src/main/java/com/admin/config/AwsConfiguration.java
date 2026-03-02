package com.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.opensearch.OpenSearchClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * AWS SDK Configuration for College Admin Service
 * Configures AWS clients for S3, SQS, SNS, CloudWatch, and Parameter Store
 */
@Configuration
public class AwsConfiguration {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public KinesisClient kinesisClient() {
        return KinesisClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SfnClient sfnClient() {
        return SfnClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SageMakerClient sageMakerClient() {
        return SageMakerClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public ApiGatewayClient apiGatewayClient() {
        return ApiGatewayClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public EventBridgeClient eventBridgeClient() {
        return EventBridgeClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public OpenSearchClient openSearchClient() {
        return OpenSearchClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public AWSXRayRecorder xrayRecorder() {
        return AWSXRayRecorderBuilder.standard()
                .withDefaultPlugins()
                .build();
    }
}