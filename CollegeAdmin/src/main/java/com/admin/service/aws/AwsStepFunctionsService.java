package com.admin.service.aws;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.CreateStateMachineRequest;
import software.amazon.awssdk.services.sfn.model.CreateStateMachineResponse;
import software.amazon.awssdk.services.sfn.model.DescribeExecutionRequest;
import software.amazon.awssdk.services.sfn.model.DescribeExecutionResponse;
import software.amazon.awssdk.services.sfn.model.ExecutionListItem;
import software.amazon.awssdk.services.sfn.model.ExecutionStatus;
import software.amazon.awssdk.services.sfn.model.GetExecutionHistoryRequest;
import software.amazon.awssdk.services.sfn.model.GetExecutionHistoryResponse;
import software.amazon.awssdk.services.sfn.model.HistoryEvent;
import software.amazon.awssdk.services.sfn.model.ListExecutionsRequest;
import software.amazon.awssdk.services.sfn.model.ListExecutionsResponse;
import software.amazon.awssdk.services.sfn.model.ListStateMachinesRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;
import software.amazon.awssdk.services.sfn.model.StateMachineType;
import software.amazon.awssdk.services.sfn.model.StopExecutionRequest;

/**
 * AWS Step Functions Workflow Orchestration Service
 * Manages complex business workflows with state machines and choreography
 */
@Profile({"aws", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsStepFunctionsService {

    private final SfnClient stepFunctionsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.stepfunctions.statemachine.student-enrollment:arn:aws:states:us-east-1:123456789012:stateMachine:StudentEnrollmentWorkflow}")
    private String studentEnrollmentStateMachineArn;

    @Value("${aws.stepfunctions.statemachine.grade-processing:arn:aws:states:us-east-1:123456789012:stateMachine:GradeProcessingWorkflow}")
    private String gradeProcessingStateMachineArn;

    @Value("${aws.stepfunctions.statemachine.notification-workflow:arn:aws:states:us-east-1:123456789012:stateMachine:NotificationWorkflow}")
    private String notificationWorkflowStateMachineArn;

    @Value("${aws.stepfunctions.statemachine.data-pipeline:arn:aws:states:us-east-1:123456789012:stateMachine:DataPipelineWorkflow}")
    private String dataPipelineStateMachineArn;

    /**
     * Start student enrollment workflow
     */
    public CompletableFuture<WorkflowExecution> startStudentEnrollmentWorkflow(StudentEnrollmentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting student enrollment workflow for: {}", request.getStudentId());

                WorkflowInput input = WorkflowInput.builder()
                        .executionId(UUID.randomUUID().toString())
                        .workflowType("STUDENT_ENROLLMENT")
                        .studentId(request.getStudentId())
                        .courseIds(request.getCourseIds())
                        .semester(request.getSemester())
                        .year(request.getYear())
                        .priority(request.getPriority())
                        .metadata(request.getMetadata())
                        .build();

                String inputJson = objectMapper.writeValueAsString(input);

                StartExecutionRequest executionRequest = StartExecutionRequest.builder()
                        .stateMachineArn(studentEnrollmentStateMachineArn)
                        .name("enrollment-" + request.getStudentId() + "-" + System.currentTimeMillis())
                        .input(inputJson)
                        .build();

                StartExecutionResponse response = stepFunctionsClient.startExecution(executionRequest);

                log.info("Student enrollment workflow started: {}", response.executionArn());

                return WorkflowExecution.builder()
                        .executionArn(response.executionArn())
                        .startDate(response.startDate())
                        .workflowType("STUDENT_ENROLLMENT")
                        .status("RUNNING")
                        .input(input)
                        .build();

            } catch (Exception e) {
                log.error("Failed to start student enrollment workflow: {}", e.getMessage());
                throw new RuntimeException("Workflow execution failed", e);
            }
        });
    }

    /**
     * Start grade processing workflow
     */
    public CompletableFuture<WorkflowExecution> startGradeProcessingWorkflow(GradeProcessingRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting grade processing workflow for course: {}", request.getCourseId());

                WorkflowInput input = WorkflowInput.builder()
                        .executionId(UUID.randomUUID().toString())
                        .workflowType("GRADE_PROCESSING")
                        .courseId(request.getCourseId())
                        .semester(request.getSemester())
                        .year(request.getYear())
                        .gradingMethod(request.getGradingMethod())
                        .approvalRequired(request.isApprovalRequired())
                        .metadata(request.getMetadata())
                        .build();

                String inputJson = objectMapper.writeValueAsString(input);

                StartExecutionRequest executionRequest = StartExecutionRequest.builder()
                        .stateMachineArn(gradeProcessingStateMachineArn)
                        .name("grades-" + request.getCourseId() + "-" + System.currentTimeMillis())
                        .input(inputJson)
                        .build();

                StartExecutionResponse response = stepFunctionsClient.startExecution(executionRequest);

                log.info("Grade processing workflow started: {}", response.executionArn());

                return WorkflowExecution.builder()
                        .executionArn(response.executionArn())
                        .startDate(response.startDate())
                        .workflowType("GRADE_PROCESSING")
                        .status("RUNNING")
                        .input(input)
                        .build();

            } catch (Exception e) {
                log.error("Failed to start grade processing workflow: {}", e.getMessage());
                throw new RuntimeException("Workflow execution failed", e);
            }
        });
    }

    /**
     * Start notification workflow
     */
    public CompletableFuture<WorkflowExecution> startNotificationWorkflow(NotificationWorkflowRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting notification workflow: {}", request.getNotificationType());

                WorkflowInput input = WorkflowInput.builder()
                        .executionId(UUID.randomUUID().toString())
                        .workflowType("NOTIFICATION")
                        .notificationType(request.getNotificationType())
                        .recipients(request.getRecipients())
                        .message(request.getMessage())
                        .channels(request.getChannels())
                        .scheduledTime(request.getScheduledTime())
                        .priority(request.getPriority())
                        .metadata(request.getMetadata())
                        .build();

                String inputJson = objectMapper.writeValueAsString(input);

                StartExecutionRequest executionRequest = StartExecutionRequest.builder()
                        .stateMachineArn(notificationWorkflowStateMachineArn)
                        .name("notification-" + request.getNotificationType() + "-" + System.currentTimeMillis())
                        .input(inputJson)
                        .build();

                StartExecutionResponse response = stepFunctionsClient.startExecution(executionRequest);

                log.info("Notification workflow started: {}", response.executionArn());

                return WorkflowExecution.builder()
                        .executionArn(response.executionArn())
                        .startDate(response.startDate())
                        .workflowType("NOTIFICATION")
                        .status("RUNNING")
                        .input(input)
                        .build();

            } catch (Exception e) {
                log.error("Failed to start notification workflow: {}", e.getMessage());
                throw new RuntimeException("Workflow execution failed", e);
            }
        });
    }

    /**
     * Start data processing pipeline workflow
     */
    public CompletableFuture<WorkflowExecution> startDataPipelineWorkflow(DataPipelineRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting data pipeline workflow: {}", request.getPipelineType());

                WorkflowInput input = WorkflowInput.builder()
                        .executionId(UUID.randomUUID().toString())
                        .workflowType("DATA_PIPELINE")
                        .pipelineType(request.getPipelineType())
                        .sourceLocation(request.getSourceLocation())
                        .destinationLocation(request.getDestinationLocation())
                        .transformationRules(request.getTransformationRules())
                        .validationRules(request.getValidationRules())
                        .metadata(request.getMetadata())
                        .build();

                String inputJson = objectMapper.writeValueAsString(input);

                StartExecutionRequest executionRequest = StartExecutionRequest.builder()
                        .stateMachineArn(dataPipelineStateMachineArn)
                        .name("pipeline-" + request.getPipelineType() + "-" + System.currentTimeMillis())
                        .input(inputJson)
                        .build();

                StartExecutionResponse response = stepFunctionsClient.startExecution(executionRequest);

                log.info("Data pipeline workflow started: {}", response.executionArn());

                return WorkflowExecution.builder()
                        .executionArn(response.executionArn())
                        .startDate(response.startDate())
                        .workflowType("DATA_PIPELINE")
                        .status("RUNNING")
                        .input(input)
                        .build();

            } catch (Exception e) {
                log.error("Failed to start data pipeline workflow: {}", e.getMessage());
                throw new RuntimeException("Workflow execution failed", e);
            }
        });
    }

    /**
     * Get workflow execution status and history
     */
    public WorkflowExecutionStatus getExecutionStatus(String executionArn) {
        try {
            DescribeExecutionRequest request = DescribeExecutionRequest.builder()
                    .executionArn(executionArn)
                    .build();

            DescribeExecutionResponse response = stepFunctionsClient.describeExecution(request);

            // Get execution history for detailed tracking
            GetExecutionHistoryRequest historyRequest = GetExecutionHistoryRequest.builder()
                    .executionArn(executionArn)
                    .maxResults(100)
                    .reverseOrder(true)
                    .build();

            GetExecutionHistoryResponse historyResponse = stepFunctionsClient.getExecutionHistory(historyRequest);

            log.info("Retrieved execution status for: {} - Status: {}", executionArn, response.status());

            return WorkflowExecutionStatus.builder()
                    .executionArn(executionArn)
                    .status(response.status().toString())
                    .startDate(response.startDate())
                    .stopDate(response.stopDate())
                    .input(response.input())
                    .output(response.output())
                    .error(response.error())
                    .cause(response.cause())
                    .executionHistory(historyResponse.events())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get execution status for {}: {}", executionArn, e.getMessage());
            return null;
        }
    }

    /**
     * Stop a running workflow execution
     */
    public boolean stopExecution(String executionArn, String reason) {
        try {
            StopExecutionRequest request = StopExecutionRequest.builder()
                    .executionArn(executionArn)
                    .cause(reason)
                    .error("USER_REQUESTED_STOP")
                    .build();

            stepFunctionsClient.stopExecution(request);
            log.info("Stopped execution: {} - Reason: {}", executionArn, reason);
            return true;

        } catch (Exception e) {
            log.error("Failed to stop execution {}: {}", executionArn, e.getMessage());
            return false;
        }
    }

    /**
     * List all running executions for a state machine
     */
    public java.util.List<ExecutionListItem> listRunningExecutions(String stateMachineArn) {
        try {
            ListExecutionsRequest request = ListExecutionsRequest.builder()
                    .stateMachineArn(stateMachineArn)
                    .statusFilter(ExecutionStatus.RUNNING)
                    .maxResults(100)
                    .build();

            ListExecutionsResponse response = stepFunctionsClient.listExecutions(request);
            
            log.info("Found {} running executions for state machine", response.executions().size());
            return response.executions();

        } catch (Exception e) {
            log.error("Failed to list executions for {}: {}", stateMachineArn, e.getMessage());
            return java.util.List.of();
        }
    }

    /**
     * Create a new state machine definition
     */
    public String createStateMachine(String name, String definition, String roleArn) {
        try {
            CreateStateMachineRequest request = CreateStateMachineRequest.builder()
                    .name(name)
                    .definition(definition)
                    .roleArn(roleArn)
                    .type(StateMachineType.STANDARD)
                    .build();

            CreateStateMachineResponse response = stepFunctionsClient.createStateMachine(request);
            
            log.info("Created state machine: {} with ARN: {}", name, response.stateMachineArn());
            return response.stateMachineArn();

        } catch (Exception e) {
            log.error("Failed to create state machine {}: {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * Get Step Functions service health status
     */
    public CompletableFuture<Map<String, Object>> getStepFunctionsHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test connection by listing state machines
                ListStateMachinesRequest request = ListStateMachinesRequest.builder()
                        .maxResults(1)
                        .build();
                
                stepFunctionsClient.listStateMachines(request);

                return Map.of(
                        "status", "UP",
                        "service", "AWS Step Functions",
                        "timestamp", java.time.Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("Step Functions health check failed", e);
                return Map.of(
                        "status", "DOWN",
                        "error", e.getMessage(),
                        "timestamp", java.time.Instant.now().toString()
                );
            }
        });
    }

    // DTOs for Step Functions workflow management
    @lombok.Data
    @lombok.Builder
    public static class WorkflowInput {
        private String executionId;
        private String workflowType;
        private String studentId;
        private java.util.List<String> courseIds;
        private String courseId;
        private String semester;
        private int year;
        private String priority;
        private String notificationType;
        private java.util.List<String> recipients;
        private String message;
        private java.util.List<String> channels;
        private String scheduledTime;
        private String pipelineType;
        private String sourceLocation;
        private String destinationLocation;
        private String gradingMethod;
        private boolean approvalRequired;
        private Map<String, Object> transformationRules;
        private Map<String, Object> validationRules;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkflowExecution {
        private String executionArn;
        private java.time.Instant startDate;
        private String workflowType;
        private String status;
        private WorkflowInput input;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkflowExecutionStatus {
        private String executionArn;
        private String status;
        private java.time.Instant startDate;
        private java.time.Instant stopDate;
        private String input;
        private String output;
        private String error;
        private String cause;
        private java.util.List<HistoryEvent> executionHistory;
    }

    @lombok.Data
    @lombok.Builder
    public static class StudentEnrollmentRequest {
        private String studentId;
        private java.util.List<String> courseIds;
        private String semester;
        private int year;
        private String priority;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class GradeProcessingRequest {
        private String courseId;
        private String semester;
        private int year;
        private String gradingMethod;
        private boolean approvalRequired;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class NotificationWorkflowRequest {
        private String notificationType;
        private java.util.List<String> recipients;
        private String message;
        private java.util.List<String> channels;
        private String scheduledTime;
        private String priority;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class DataPipelineRequest {
        private String pipelineType;
        private String sourceLocation;
        private String destinationLocation;
        private Map<String, Object> transformationRules;
        private Map<String, Object> validationRules;
        private Map<String, Object> metadata;
    }
}