package com.admin.service.aws;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
import software.amazon.awssdk.services.lambda.model.LogType;

/**
 * AWS Lambda Integration Service
 * Provides serverless function invocation and management capabilities
 */
@Profile({"aws", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsLambdaService {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.lambda.functions.student-processor:college-student-processor}")
    private String studentProcessorFunction;

    @Value("${aws.lambda.functions.notification-sender:college-notification-sender}")
    private String notificationSenderFunction;

    @Value("${aws.lambda.functions.data-validator:college-data-validator}")
    private String dataValidatorFunction;

    @Value("${aws.lambda.functions.report-generator:college-report-generator}")
    private String reportGeneratorFunction;

    /**
     * Process student data using serverless Lambda function
     */
    public CompletableFuture<StudentProcessingResult> processStudentData(StudentData studentData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Processing student data via Lambda: {}", studentData.getStudentId());

                String payload = objectMapper.writeValueAsString(Map.of(
                    "action", "process_student",
                    "studentData", studentData,
                    "timestamp", System.currentTimeMillis(),
                    "source", "college-admin-service"
                ));

                InvokeRequest invokeRequest = InvokeRequest.builder()
                        .functionName(studentProcessorFunction)
                        .invocationType(InvocationType.REQUEST_RESPONSE)
                        .logType(LogType.TAIL)
                        .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                        .build();

                InvokeResponse response = lambdaClient.invoke(invokeRequest);
                
                if (response.statusCode() == 200) {
                    String responsePayload = response.payload().asUtf8String();
                    StudentProcessingResult result = objectMapper.readValue(responsePayload, StudentProcessingResult.class);
                    
                    log.info("Student processing completed successfully: {}", studentData.getStudentId());
                    return result;
                } else {
                    throw new RuntimeException("Lambda function failed with status: " + response.statusCode());
                }

            } catch (Exception e) {
                log.error("Failed to process student data via Lambda: {}", e.getMessage());
                throw new RuntimeException("Lambda processing failed", e);
            }
        });
    }

    /**
     * Send notifications using serverless function
     */
    public CompletableFuture<Boolean> sendNotificationAsync(NotificationRequest notification) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Sending notification via Lambda: {}", notification.getType());

                String payload = objectMapper.writeValueAsString(Map.of(
                    "action", "send_notification",
                    "notification", notification,
                    "timestamp", System.currentTimeMillis()
                ));

                InvokeRequest invokeRequest = InvokeRequest.builder()
                        .functionName(notificationSenderFunction)
                        .invocationType(InvocationType.EVENT) // Async invocation
                        .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                        .build();

                InvokeResponse response = lambdaClient.invoke(invokeRequest);
                boolean success = response.statusCode() == 202; // Async invocation success status
                
                log.info("Notification sent via Lambda: {} - Success: {}", notification.getType(), success);
                return success;

            } catch (Exception e) {
                log.error("Failed to send notification via Lambda: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Validate data using Lambda function
     */
    public DataValidationResult validateData(Object data, String validationType) {
        try {
            log.info("Validating data via Lambda: {}", validationType);

            String payload = objectMapper.writeValueAsString(Map.of(
                "action", "validate_data",
                "data", data,
                "validationType", validationType,
                "timestamp", System.currentTimeMillis()
            ));

            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(dataValidatorFunction)
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                    .build();

            InvokeResponse response = lambdaClient.invoke(invokeRequest);
            
            if (response.statusCode() == 200) {
                String responsePayload = response.payload().asUtf8String();
                DataValidationResult result = objectMapper.readValue(responsePayload, DataValidationResult.class);
                
                log.info("Data validation completed: {} - Valid: {}", validationType, result.isValid());
                return result;
            } else {
                throw new RuntimeException("Validation Lambda failed with status: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("Failed to validate data via Lambda: {}", e.getMessage());
            return DataValidationResult.builder()
                    .valid(false)
                    .errorMessage("Lambda validation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Generate reports using serverless function
     */
    public CompletableFuture<ReportGenerationResult> generateReportAsync(ReportRequest reportRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating report via Lambda: {}", reportRequest.getReportType());

                String payload = objectMapper.writeValueAsString(Map.of(
                    "action", "generate_report",
                    "reportRequest", reportRequest,
                    "timestamp", System.currentTimeMillis()
                ));

                InvokeRequest invokeRequest = InvokeRequest.builder()
                        .functionName(reportGeneratorFunction)
                        .invocationType(InvocationType.REQUEST_RESPONSE)
                        .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                        .build();

                InvokeResponse response = lambdaClient.invoke(invokeRequest);
                
                if (response.statusCode() == 200) {
                    String responsePayload = response.payload().asUtf8String();
                    ReportGenerationResult result = objectMapper.readValue(responsePayload, ReportGenerationResult.class);
                    
                    log.info("Report generation completed: {}", reportRequest.getReportType());
                    return result;
                } else {
                    throw new RuntimeException("Report Lambda failed with status: " + response.statusCode());
                }

            } catch (Exception e) {
                log.error("Failed to generate report via Lambda: {}", e.getMessage());
                throw new RuntimeException("Lambda report generation failed", e);
            }
        });
    }

    /**
     * List all Lambda functions in the account
     */
    public void listLambdaFunctions() {
        try {
            ListFunctionsRequest request = ListFunctionsRequest.builder().build();
            ListFunctionsResponse response = lambdaClient.listFunctions(request);
            
            log.info("Available Lambda functions:");
            response.functions().forEach(function -> 
                log.info("  - {}: {} (Runtime: {}, Memory: {}MB)", 
                    function.functionName(), 
                    function.description(), 
                    function.runtime(), 
                    function.memorySize())
            );

        } catch (Exception e) {
            log.error("Failed to list Lambda functions: {}", e.getMessage());
        }
    }

    /**
     * Get Lambda service health status
     */
    public CompletableFuture<Map<String, Object>> getLambdaHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test connection by listing functions
                ListFunctionsRequest request = ListFunctionsRequest.builder()
                        .maxItems(1)
                        .build();
                
                lambdaClient.listFunctions(request);

                return Map.of(
                        "status", "UP",
                        "service", "AWS Lambda",
                        "timestamp", java.time.Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("Lambda health check failed", e);
                return Map.of(
                        "status", "DOWN",
                        "error", e.getMessage(),
                        "timestamp", java.time.Instant.now().toString()
                );
            }
        });
    }

    // DTOs for Lambda communication
    @lombok.Data
    @lombok.Builder
    public static class StudentData {
        private String studentId;
        private String name;
        private String email;
        private String department;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class StudentProcessingResult {
        private String studentId;
        private boolean processed;
        private String status;
        private String message;
        private Map<String, Object> results;
    }

    @lombok.Data
    @lombok.Builder
    public static class NotificationRequest {
        private String type;
        private String recipient;
        private String subject;
        private String message;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class DataValidationResult {
        private boolean valid;
        private String errorMessage;
        private Map<String, Object> validationDetails;
    }

    @lombok.Data
    @lombok.Builder
    public static class ReportRequest {
        private String reportType;
        private Map<String, Object> parameters;
        private String format;
        private String outputLocation;
    }

    @lombok.Data
    @lombok.Builder
    public static class ReportGenerationResult {
        private String reportId;
        private String status;
        private String downloadUrl;
        private long generationTime;
        private Map<String, Object> metadata;
    }
}