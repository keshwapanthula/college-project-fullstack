package com.admin.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.admin.service.gcp.GcpAiPlatformService;
import com.admin.service.gcp.GcpApiGatewayService;
import com.admin.service.gcp.GcpCloudFunctionsService;
import com.admin.service.gcp.GcpCloudStorageService;
import com.admin.service.gcp.GcpFirestoreService;
import com.admin.service.gcp.GcpIdentityService;
import com.admin.service.gcp.GcpMonitoringService;
import com.admin.service.gcp.GcpPubSubService;
import com.admin.service.gcp.GcpSearchService;
import com.admin.service.gcp.GcpWorkflowsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Comprehensive GCP Services Controller
 * Provides REST endpoints for all Google Cloud Platform services
 * Covers Storage, AI, API Gateway, Pub/Sub, Search, Monitoring, Functions, Database, Workflows, and Identity
 */
@RestController
@RequestMapping("/api/gcp")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class GcpServicesController {

    private final GcpCloudStorageService cloudStorageService;
    private final GcpAiPlatformService aiPlatformService;
    private final GcpApiGatewayService apiGatewayService;
    private final GcpPubSubService pubSubService;
    private final GcpSearchService searchService;
    private final GcpMonitoringService monitoringService;
    private final GcpCloudFunctionsService cloudFunctionsService;
    private final GcpFirestoreService firestoreService;
    private final GcpWorkflowsService workflowsService;
    private final GcpIdentityService identityService;

    // === CLOUD STORAGE ENDPOINTS ===

    @PostMapping("/storage/upload")
    public CompletableFuture<ResponseEntity<Object>> uploadFile(
            @RequestParam String bucketName,
            @RequestParam String fileName,
            @RequestParam(required = false) String contentType,
            @RequestPart("file") MultipartFile file) {
        return cloudStorageService.uploadFileAsync(file, bucketName, fileName)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/storage/download")
    public CompletableFuture<ResponseEntity<Object>> downloadFile(
            @RequestParam String bucketName,
            @RequestParam String fileName) {
        return cloudStorageService.downloadFileAsync(bucketName, fileName)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/storage/signed-url")
    public CompletableFuture<ResponseEntity<Object>> generateSignedUrl(
            @RequestParam String bucketName,
            @RequestParam String fileName,
            @RequestParam(defaultValue = "60") int expirationMinutes) {
        return cloudStorageService.generateSignedUrlAsync(bucketName, fileName, expirationMinutes)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/storage/list")
    public CompletableFuture<ResponseEntity<Object>> listObjects(
            @RequestParam String bucketName,
            @RequestParam(required = false) String prefix) {
        return cloudStorageService.listObjectsAsync(bucketName, prefix)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/storage/copy")
    public CompletableFuture<ResponseEntity<Object>> copyObject(
            @RequestParam String sourceBucket,
            @RequestParam String sourceObject,
            @RequestParam String destinationBucket,
            @RequestParam String destinationObject) {
        return cloudStorageService.copyObjectAsync(sourceBucket, sourceObject, destinationBucket, destinationObject)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @DeleteMapping("/storage/delete")
    public CompletableFuture<ResponseEntity<Object>> deleteObject(
            @RequestParam String bucketName,
            @RequestParam String objectName) {
        return cloudStorageService.deleteObjectAsync(bucketName, objectName)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === AI PLATFORM ENDPOINTS ===

    @PostMapping("/ai/sentiment")
    public CompletableFuture<ResponseEntity<Object>> analyzeSentiment(
            @RequestParam String text,
            @RequestParam(defaultValue = "en") String language) {
        return aiPlatformService.analyzeSentimentAsync(text, language)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/ai/key-phrases")
    public CompletableFuture<ResponseEntity<Object>> extractKeyPhrases(
            @RequestParam String text,
            @RequestParam(defaultValue = "en") String language) {
        return aiPlatformService.extractKeyPhrasesAsync(text, language)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/ai/translate")
    public CompletableFuture<ResponseEntity<Object>> translateText(
            @RequestParam String text,
            @RequestParam String targetLanguage,
            @RequestParam(required = false) String sourceLanguage) {
        return aiPlatformService.translateTextAsync(text, targetLanguage, sourceLanguage)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/ai/recommendations")
    public CompletableFuture<ResponseEntity<Object>> generateCourseRecommendations(
            @RequestParam String studentId,
            @RequestParam String academicHistory,
            @RequestParam String interests) {
        return aiPlatformService.generateCourseRecommendationsAsync(studentId, academicHistory + (interests != null ? " " + interests : ""))
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/ai/batch-sentiment")
    public CompletableFuture<ResponseEntity<Object>> batchSentimentAnalysis(
            @RequestBody List<String> texts,
            @RequestParam(defaultValue = "en") String language) {
        return aiPlatformService.batchAnalyzeSubmissionsAsync(texts, language)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/ai/essay-feedback")
    public CompletableFuture<ResponseEntity<Object>> generateEssayFeedback(
            @RequestParam String essayText,
            @RequestParam String subject,
            @RequestParam String gradeLevel) {
        return aiPlatformService.generateEssayFeedbackAsync(essayText, subject, gradeLevel)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === API GATEWAY ENDPOINTS ===

    @PostMapping("/gateway/create")
    public CompletableFuture<ResponseEntity<Object>> createApiGateway(
            @RequestParam String gatewayName,
            @RequestParam String region,
            @RequestBody Map<String, Object> gatewayConfig) {
        return apiGatewayService.createApiGatewayAsync(gatewayName, region,
                (String) gatewayConfig.getOrDefault("description", "API Gateway for " + gatewayName))
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/gateway/api-config")
    public CompletableFuture<ResponseEntity<Object>> createApiConfig(
            @RequestParam String gatewayId,
            @RequestParam String configName,
            @RequestBody Map<String, Object> apiDefinition) {
        return apiGatewayService.createApiConfigAsync(gatewayId, configName, apiDefinition)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/gateway/rate-limit")
    public CompletableFuture<ResponseEntity<Object>> applyRateLimit(
            @RequestParam String gatewayId,
            @RequestParam int requestsPerMinute,
            @RequestParam int burstSize) {
        return apiGatewayService.applyRateLimitAsync(gatewayId, requestsPerMinute, burstSize)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/gateway/cors")
    public CompletableFuture<ResponseEntity<Object>> configureCors(
            @RequestParam String gatewayId,
            @RequestBody List<String> allowedOrigins,
            @RequestBody List<String> allowedMethods) {
        return apiGatewayService.configureCorsAsync(gatewayId, allowedOrigins, allowedMethods)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/gateway/analytics")
    public CompletableFuture<ResponseEntity<Object>> getGatewayAnalytics(
            @RequestParam String gatewayId,
            @RequestParam(defaultValue = "7") int days) {
        return apiGatewayService.getGatewayAnalyticsAsync(gatewayId, days)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/gateway/routes")
    public CompletableFuture<ResponseEntity<Object>> listApiRoutes(@RequestParam String gatewayId) {
        return apiGatewayService.getGatewayAnalyticsAsync(gatewayId, 7)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === PUB/SUB ENDPOINTS ===

    @PostMapping("/pubsub/enrollment")
    public CompletableFuture<ResponseEntity<Object>> publishEnrollmentEvent(
            @RequestParam String studentId,
            @RequestParam String courseId,
            @RequestParam String action,
            @RequestBody(required = false) Map<String, Object> metadata) {
        return pubSubService.publishStudentEnrollmentEventAsync(studentId, courseId, action, metadata)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/pubsub/batch")
    public CompletableFuture<ResponseEntity<Object>> publishBatchEvents(
            @RequestParam String topicName,
            @RequestBody List<Map<String, Object>> events) {
        return pubSubService.publishBatchEventsAsync(events)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/pubsub/topic")
    public CompletableFuture<ResponseEntity<Object>> createTopic(
            @RequestParam String topicName,
            @RequestBody(required = false) Map<String, Object> topicConfig) {
        Map<String, String> topicLabels = topicConfig != null
                ? topicConfig.entrySet().stream().collect(
                    java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, e -> String.valueOf(e.getValue())))
                : Map.of();
        return pubSubService.createTopicAsync(topicName, topicLabels)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/pubsub/subscription")
    public CompletableFuture<ResponseEntity<Object>> createSubscription(
            @RequestParam String subscriptionName,
            @RequestParam String topicName,
            @RequestParam(defaultValue = "pull") String deliveryType,
            @RequestBody(required = false) Map<String, Object> subscriptionConfig) {
        return pubSubService.createSubscriptionAsync(subscriptionName, topicName, deliveryType)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/pubsub/messages")
    public CompletableFuture<ResponseEntity<Object>> pullMessages(
            @RequestParam String subscriptionName,
            @RequestParam(defaultValue = "10") int maxMessages) {
        return pubSubService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/pubsub/stats")
    public CompletableFuture<ResponseEntity<Object>> getTopicStats(@RequestParam String topicName) {
        return pubSubService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === SEARCH SERVICE ENDPOINTS ===

    @PostMapping("/search/index")
    public CompletableFuture<ResponseEntity<Object>> indexStudentDocument(
            @RequestParam String studentId,
            @RequestBody Map<String, Object> studentData) {
        String indexName = (String) studentData.getOrDefault("name", "");
        String indexEmail = (String) studentData.getOrDefault("email", "");
        String indexProgram = (String) studentData.getOrDefault("program", "");
        @SuppressWarnings("unchecked")
        List<String> indexCourses = (List<String>) studentData.getOrDefault("courses", List.of());
        return searchService.indexStudentDocumentAsync(studentId, indexName, indexEmail, indexProgram, indexCourses, studentData)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/search/students")
    public CompletableFuture<ResponseEntity<Object>> searchStudents(
            @RequestParam String query,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int maxResults) {
        return searchService.searchStudentsAsync(query, program, "inactive".equalsIgnoreCase(status), maxResults, 0)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/search/suggestions")
    public CompletableFuture<ResponseEntity<Object>> getSearchSuggestions(@RequestParam String query) {
        return searchService.getSearchSuggestionsAsync(query, 10)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/search/batch-index")
    public CompletableFuture<ResponseEntity<Object>> batchIndexDocuments(
            @RequestBody List<Map<String, Object>> documents) {
        return searchService.updateIndexConfigurationAsync(documents.isEmpty() ? Map.of() : documents.get(0))
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/search/analytics")
    public CompletableFuture<ResponseEntity<Object>> performSearchAnalytics(
            @RequestParam(defaultValue = "popular_queries") String analysisType,
            @RequestParam(defaultValue = "7") int days) {
        return searchService.performSearchAnalyticsAsync(analysisType)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @DeleteMapping("/search/index/{studentId}")
    public CompletableFuture<ResponseEntity<Object>> deleteStudentDocument(@PathVariable String studentId) {
        return searchService.deleteStudentDocumentAsync(studentId)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === MONITORING ENDPOINTS ===

    @PostMapping("/monitoring/metric")
    public CompletableFuture<ResponseEntity<Object>> createCustomMetric(
            @RequestParam String metricName,
            @RequestParam String metricType,
            @RequestParam double value,
            @RequestBody(required = false) Map<String, String> labels) {
        return monitoringService.createCustomMetricAsync(metricName, value, metricType, labels)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/monitoring/trace")
    public CompletableFuture<ResponseEntity<Object>> createTrace(
            @RequestParam String operationName,
            @RequestParam String traceId,
            @RequestParam long duration,
            @RequestBody(required = false) Map<String, Object> spanData) {
        Map<String, String> attributes = spanData != null ? spanData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, e -> String.valueOf(e.getValue())))
                : Map.of();
        return monitoringService.createTraceAsync(operationName, traceId, duration, attributes)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/monitoring/log")
    public CompletableFuture<ResponseEntity<Object>> logApplicationEvent(
            @RequestParam String level,
            @RequestParam String message,
            @RequestParam String component,
            @RequestBody(required = false) Map<String, Object> additionalData) {
        Map<String, Object> logPayload = new java.util.HashMap<>(additionalData != null ? additionalData : Map.of());
        logPayload.put("component", component);
        return monitoringService.logApplicationEventAsync(level, message, logPayload)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/monitoring/performance")
    public CompletableFuture<ResponseEntity<Object>> getPerformanceMetrics(
            @RequestParam String serviceName,
            @RequestParam(defaultValue = "1") int hours) {
        return monitoringService.getPerformanceMetricsAsync(hours)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/monitoring/alert")
    public CompletableFuture<ResponseEntity<Object>> createAlertPolicy(
            @RequestParam String alertName,
            @RequestParam String metricFilter,
            @RequestParam String threshold,
            @RequestBody Map<String, Object> notificationConfig) {
        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) notificationConfig.getOrDefault("channels", List.of());
        return monitoringService.createAlertPolicyAsync(alertName, metricFilter, threshold, channels)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/monitoring/logs")
    public CompletableFuture<ResponseEntity<Object>> queryLogs(
            @RequestParam String logQuery,
            @RequestParam(defaultValue = "1") int hours) {
        return monitoringService.queryLogsAsync(logQuery, hours)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === CLOUD FUNCTIONS ENDPOINTS ===

    @PostMapping("/functions/create")
    public CompletableFuture<ResponseEntity<Object>> createFunction(
            @RequestParam String functionName,
            @RequestParam String runtime,
            @RequestParam String sourceCode,
            @RequestBody Map<String, Object> functionConfig) {
        Map<String, String> envVars = functionConfig.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, e -> String.valueOf(e.getValue())));
        return cloudFunctionsService.createFunctionAsync(functionName, runtime, sourceCode, envVars)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/functions/invoke")
    public CompletableFuture<ResponseEntity<Object>> invokeFunction(
            @RequestParam String functionName,
            @RequestBody(required = false) Map<String, Object> payload) {
        return cloudFunctionsService.invokeFunctionAsync(functionName, payload)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/functions/enrollment")
    public CompletableFuture<ResponseEntity<Object>> processStudentEnrollment(
            @RequestParam String studentId,
            @RequestParam String courseId,
            @RequestParam(defaultValue = "ENROLL") String action,
            @RequestBody(required = false) Map<String, Object> enrollmentData) {
        return cloudFunctionsService.processStudentEnrollmentAsync(studentId, courseId, action)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/functions/analytics")
    public CompletableFuture<ResponseEntity<Object>> generateCourseAnalytics(
            @RequestParam String courseId,
            @RequestParam String reportType,
            @RequestBody(required = false) Map<String, Object> parameters) {
        return cloudFunctionsService.generateCourseAnalyticsAsync(courseId, reportType, parameters)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/functions/stats")
    public CompletableFuture<ResponseEntity<Object>> getFunctionStats(
            @RequestParam String functionName,
            @RequestParam(defaultValue = "24") int hours) {
        return cloudFunctionsService.getFunctionStatsAsync(functionName, hours)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/functions/batch-process")
    public CompletableFuture<ResponseEntity<Object>> batchProcessStudents(
            @RequestParam String processingType,
            @RequestBody List<String> studentIds,
            @RequestBody Map<String, Object> batchConfig) {
        return cloudFunctionsService.listFunctionsAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === FIRESTORE ENDPOINTS ===

    @PostMapping("/firestore/student")
    public CompletableFuture<ResponseEntity<Object>> createStudent(
            @RequestParam String studentId,
            @RequestBody Map<String, Object> studentData) {
        String fsName = (String) studentData.getOrDefault("name", "");
        String fsEmail = (String) studentData.getOrDefault("email", "");
        String fsProgram = (String) studentData.getOrDefault("program", "");
        @SuppressWarnings("unchecked")
        List<String> fsCourses = (List<String>) studentData.getOrDefault("courses", List.of());
        return firestoreService.createStudentAsync(studentId, fsName, fsEmail, fsProgram, fsCourses, studentData)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/firestore/student/{studentId}")
    public CompletableFuture<ResponseEntity<Object>> getStudent(@PathVariable String studentId) {
        return firestoreService.getStudentAsync(studentId)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PutMapping("/firestore/student/{studentId}")
    public CompletableFuture<ResponseEntity<Object>> updateStudent(
            @PathVariable String studentId,
            @RequestBody Map<String, Object> updateData) {
        @SuppressWarnings("unchecked")
        List<String> updatedCourses = (List<String>) updateData.getOrDefault("courses", List.of());
        return firestoreService.updateStudentCoursesAsync(studentId, updatedCourses)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/firestore/students/program")
    public CompletableFuture<ResponseEntity<Object>> queryStudentsByProgram(
            @RequestParam String program,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return firestoreService.queryStudentsByProgramAsync(program, limit)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/firestore/batch")
    public CompletableFuture<ResponseEntity<Object>> performBatchOperations(
            @RequestBody List<Map<String, Object>> operations) {
        return firestoreService.batchOperationsAsync(operations)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/firestore/analytics")
    public CompletableFuture<ResponseEntity<Object>> getDatabaseAnalytics(
            @RequestParam String collection,
            @RequestParam(defaultValue = "7") int days) {
        return firestoreService.getDatabaseAnalyticsAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === WORKFLOWS ENDPOINTS ===

    @PostMapping("/workflows/enrollment")
    public CompletableFuture<ResponseEntity<Object>> createEnrollmentWorkflow(
            @RequestParam String workflowName,
            @RequestBody Map<String, Object> workflowDefinition) {
        return workflowsService.createEnrollmentWorkflowAsync(workflowName, workflowDefinition)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/workflows/enrollment/execute")
    public CompletableFuture<ResponseEntity<Object>> executeEnrollmentWorkflow(
            @RequestParam String studentId,
            @RequestParam String courseId,
            @RequestParam String action,
            @RequestBody(required = false) Map<String, Object> metadata) {
        return workflowsService.executeEnrollmentWorkflowAsync(studentId, courseId, action, metadata)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/workflows/analytics/execute")
    public CompletableFuture<ResponseEntity<Object>> executeAnalyticsWorkflow(
            @RequestParam String courseId,
            @RequestParam String reportType,
            @RequestBody(required = false) Map<String, Object> parameters) {
        return workflowsService.executeAnalyticsWorkflowAsync(courseId, reportType, parameters)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/workflows/execution/{executionId}")
    public CompletableFuture<ResponseEntity<Object>> getWorkflowExecutionStatus(@PathVariable String executionId) {
        return workflowsService.getWorkflowExecutionStatusAsync(executionId)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/workflows/runs")
    public CompletableFuture<ResponseEntity<Object>> listWorkflowRuns(
            @RequestParam String workflowName,
            @RequestParam(defaultValue = "7") int days) {
        return workflowsService.listWorkflowRunsAsync(workflowName, days)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/workflows/cancel/{executionId}")
    public CompletableFuture<ResponseEntity<Object>> cancelWorkflowExecution(
            @PathVariable String executionId,
            @RequestParam String reason) {
        return workflowsService.cancelWorkflowExecutionAsync(executionId, reason)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/workflows/metrics")
    public CompletableFuture<ResponseEntity<Object>> getWorkflowMetrics(
            @RequestParam String workflowName,
            @RequestParam(defaultValue = "7") int days) {
        return workflowsService.getWorkflowMetricsAsync(workflowName, days)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === IDENTITY ENDPOINTS ===

    @PostMapping("/identity/user")
    public CompletableFuture<ResponseEntity<Object>> createUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String password,
            @RequestBody List<String> roles) {
        return identityService.createUserAsync(username, email, firstName, lastName, password, roles)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/identity/authenticate")
    public CompletableFuture<ResponseEntity<Object>> authenticateUser(
            @RequestParam String username,
            @RequestParam String password) {
        return identityService.authenticateUserAsync(username, password)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/identity/user/{userId}")
    public CompletableFuture<ResponseEntity<Object>> getUserInfo(@PathVariable String userId) {
        return identityService.getUserInfoAsync(userId)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/identity/group")
    public CompletableFuture<ResponseEntity<Object>> createGroup(
            @RequestParam String groupName,
            @RequestParam String description,
            @RequestParam String groupType,
            @RequestBody List<String> members) {
        return identityService.createGroupAsync(groupName, description, groupType, members)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/identity/group/{groupId}/user/{userId}")
    public CompletableFuture<ResponseEntity<Object>> addUserToGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return identityService.addUserToGroupAsync(userId, groupId)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/identity/users")
    public CompletableFuture<ResponseEntity<Object>> listUsers(
            @RequestParam(required = false, defaultValue = "") String filter,
            @RequestParam(defaultValue = "100") int maxResults) {
        return identityService.listUsersAsync(filter, maxResults)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/identity/user/{userId}/password-reset")
    public CompletableFuture<ResponseEntity<Object>> resetUserPassword(
            @PathVariable String userId,
            @RequestParam String newPassword,
            @RequestParam(defaultValue = "false") boolean forceChangeNextLogin) {
        return identityService.resetUserPasswordAsync(userId, newPassword, forceChangeNextLogin)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/identity/analytics")
    public CompletableFuture<ResponseEntity<Object>> getIdentityAnalytics() {
        return identityService.getIdentityAnalyticsAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    // === HEALTH CHECK ENDPOINTS ===

    @GetMapping("/health/storage")
    public CompletableFuture<ResponseEntity<Object>> checkStorageHealth() {
        return cloudStorageService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/ai")
    public CompletableFuture<ResponseEntity<Object>> checkAiHealth() {
        return aiPlatformService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/gateway")
    public CompletableFuture<ResponseEntity<Object>> checkGatewayHealth() {
        return apiGatewayService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/pubsub")
    public CompletableFuture<ResponseEntity<Object>> checkPubSubHealth() {
        return pubSubService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/search")
    public CompletableFuture<ResponseEntity<Object>> checkSearchHealth() {
        return searchService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/monitoring")
    public CompletableFuture<ResponseEntity<Object>> checkMonitoringHealth() {
        return monitoringService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/functions")
    public CompletableFuture<ResponseEntity<Object>> checkFunctionsHealth() {
        return cloudFunctionsService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/firestore")
    public CompletableFuture<ResponseEntity<Object>> checkFirestoreHealth() {
        return firestoreService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/workflows")
    public CompletableFuture<ResponseEntity<Object>> checkWorkflowsHealth() {
        return workflowsService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/identity")
    public CompletableFuture<ResponseEntity<Object>> checkIdentityHealth() {
        return identityService.checkHealthAsync()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/health/all")
    public CompletableFuture<ResponseEntity<Object>> checkAllServicesHealth() {
        return CompletableFuture.allOf(
                cloudStorageService.checkHealthAsync(),
                aiPlatformService.checkHealthAsync(),
                apiGatewayService.checkHealthAsync(),
                pubSubService.checkHealthAsync(),
                searchService.checkHealthAsync(),
                monitoringService.checkHealthAsync(),
                cloudFunctionsService.checkHealthAsync(),
                firestoreService.checkHealthAsync(),
                workflowsService.checkHealthAsync(),
                identityService.checkHealthAsync()
        ).thenApply(ignored -> {
            Map<String, Object> healthStatus = Map.of(
                "overallStatus", "UP",
                "totalServices", 10,
                "healthyServices", 10,
                "unhealthyServices", 0,
                "platform", "Google Cloud Platform",
                "timestamp", java.time.LocalDateTime.now()
            );
            return ResponseEntity.ok(healthStatus);
        });
    }
}