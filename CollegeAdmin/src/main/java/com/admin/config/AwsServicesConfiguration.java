package com.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.opensearch.OpenSearchClient;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;
import software.amazon.awssdk.services.sfn.SfnClient;

/**
 * Comprehensive AWS Services Configuration
 * Configures all major AWS services for enterprise Java applications
 */
@Configuration
public class AwsServicesConfiguration {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
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
    public KinesisClient kinesisClient() {
        return KinesisClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public RdsClient rdsClient() {
        return RdsClient.builder()
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
    public SfnClient stepFunctionsClient() {
        return SfnClient.builder()
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
}