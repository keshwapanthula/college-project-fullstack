package com.admin.controller;

import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.admin.service.azure.AzureActiveDirectoryService;
import com.admin.service.azure.AzureApiManagementService;
import com.admin.service.azure.AzureApplicationInsightsService;
import com.admin.service.azure.AzureBlobStorageService;
import com.admin.service.azure.AzureCognitiveServicesService;
import com.admin.service.azure.AzureCosmosDbService;
import com.admin.service.azure.AzureEventGridService;
import com.admin.service.azure.AzureFunctionsService;
import com.admin.service.azure.AzureLogicAppsService;
import com.admin.service.azure.AzureSearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure Services Controller
 * Comprehensive REST API controller exposing all 10 Azure services
 * Equivalent to AWS services but for Azure cloud platform
 */
@Profile({"azure", "default"})
@RestController
@RequestMapping("/api/azure")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AzureServicesController {

    // Azure Services
    private final AzureBlobStorageService azureBlobStorageService;
    private final AzureCognitiveServicesService azureCognitiveServicesService;
    private final AzureApiManagementService azureApiManagementService;
    private final AzureEventGridService azureEventGridService;
    private final AzureSearchService azureSearchService;
    private final AzureApplicationInsightsService azureApplicationInsightsService;
    private final AzureFunctionsService azureFunctionsService;
    private final AzureCosmosDbService azureCosmosDbService;
    private final AzureLogicAppsService azureLogicAppsService;
    private final AzureActiveDirectoryService azureActiveDirectoryService;

    // ============= AZURE BLOB STORAGE ENDPOINTS =============

    @PostMapping("/blob-storage/upload")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> uploadBlob(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "container", defaultValue = "college-admin-container") String containerName,
            @RequestParam("blobName") String blobName) {
        
        return azureBlobStorageService.uploadFileAsync(file, containerName, blobName)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/blob-storage/sas-token")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateSasToken(
            @RequestParam("container") String containerName,
            @RequestParam("blobName") String blobName,
            @RequestParam(value = "duration", defaultValue = "24") int durationHours) {
        
        return azureBlobStorageService.generateSasTokenAsync(containerName, blobName, durationHours)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/blob-storage/list")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> listBlobs(
            @RequestParam("container") String containerName,
            @RequestParam(value = "prefix", required = false) String prefix) {
        
        return azureBlobStorageService.listBlobsAsync(containerName, prefix)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/blob-storage/copy")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> copyBlob(
            @RequestParam("sourceContainer") String sourceContainer,
            @RequestParam("sourceBlobName") String sourceBlobName,
            @RequestParam("targetContainer") String targetContainer,
            @RequestParam("targetBlobName") String targetBlobName) {
        
        return azureBlobStorageService.copyBlobAsync(sourceContainer, sourceBlobName, targetContainer, targetBlobName)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/blob-storage/properties")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getBlobProperties(
            @RequestParam("container") String containerName,
            @RequestParam("blobName") String blobName) {
        
        return azureBlobStorageService.getBlobPropertiesAsync(containerName, blobName)
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE COGNITIVE SERVICES ENDPOINTS =============

    @PostMapping("/cognitive/sentiment")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> analyzeSentiment(
            @RequestParam("text") String text,
            @RequestParam(value = "context", defaultValue = "general") String context) {
        
        return azureCognitiveServicesService.analyzeSentimentAsync(text, context)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/cognitive/key-phrases")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> extractKeyPhrases(
            @RequestParam("text") String text,
            @RequestParam(value = "documentType", defaultValue = "general") String documentType) {
        
        return azureCognitiveServicesService.extractKeyPhrasesAsync(text, documentType)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/cognitive/language")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> detectLanguage(
            @RequestParam("text") String text) {
        
        return azureCognitiveServicesService.detectLanguageAsync(text)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/cognitive/entities")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> recognizeEntities(
            @RequestParam("text") String text,
            @RequestParam(value = "context", defaultValue = "general") String context) {
        
        return azureCognitiveServicesService.recognizeEntitiesAsync(text, context)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/cognitive/course-recommendations")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateCourseRecommendations(
            @RequestParam("studentProfile") String studentProfile,
            @RequestParam("preferences") String preferences) {
        
        return azureCognitiveServicesService.generateCourseRecommendationsAsync(studentProfile, preferences)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/cognitive/essay-feedback")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateEssayFeedback(
            @RequestParam("essay") String essay,
            @RequestParam("assignment") String assignment,
            @RequestParam("criteria") String criteria) {
        
        return azureCognitiveServicesService.generateEssayFeedbackAsync(essay, assignment, criteria)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/cognitive/batch-analyze")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> batchAnalyzeSubmissions(
            @RequestBody List<String> submissions,
            @RequestParam("analysisType") String analysisType) {
        
        return azureCognitiveServicesService.batchAnalyzeSubmissionsAsync(submissions, analysisType)
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE API MANAGEMENT ENDPOINTS =============

    @PostMapping("/apim/service")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createApiManagementService(
            @RequestParam("serviceName") String serviceName,
            @RequestParam("sku") String sku,
            @RequestParam("region") String region) {
        
        return azureApiManagementService.createApiManagementServiceAsync(serviceName, sku, region)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/apim/api")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createApi(
            @RequestParam("apiName") String apiName,
            @RequestParam("path") String path,
            @RequestParam("backendUrl") String backendUrl,
            @RequestParam("description") String description) {
        
        return azureApiManagementService.createApiAsync(apiName, path, backendUrl, description)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/apim/operation")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createApiOperation(
            @RequestParam("apiName") String apiName,
            @RequestParam("operationName") String operationName,
            @RequestParam("httpMethod") String httpMethod,
            @RequestParam("urlTemplate") String urlTemplate,
            @RequestParam("description") String description) {
        
        return azureApiManagementService.createApiOperationAsync(apiName, operationName, httpMethod, urlTemplate, description)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/apim/subscription")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createSubscription(
            @RequestParam("subscriptionName") String subscriptionName,
            @RequestParam("scope") String scope,
            @RequestParam("displayName") String displayName,
            @RequestParam("description") String description) {
        
        return azureApiManagementService.createSubscriptionAsync(subscriptionName, scope, displayName, description)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/apim/rate-limit")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> applyRateLimitPolicy(
            @RequestParam("apiName") String apiName,
            @RequestParam("callsPerPeriod") int callsPerPeriod,
            @RequestParam("renewalPeriodSeconds") int renewalPeriodSeconds) {
        
        return azureApiManagementService.applyRateLimitPolicyAsync(apiName, callsPerPeriod, renewalPeriodSeconds)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/apim/analytics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getApiAnalytics(
            @RequestParam("apiName") String apiName,
            @RequestParam(value = "days", defaultValue = "7") int daysPast) {
        
        return azureApiManagementService.getApiAnalyticsAsync(apiName, daysPast)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/apim/apis")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> listApis() {
        return azureApiManagementService.listApisAsync()
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE EVENT GRID ENDPOINTS =============

    @PostMapping("/event-grid/student-enrollment")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> publishStudentEnrollmentEvent(
            @RequestParam("studentId") String studentId,
            @RequestParam("courseId") String courseId,
            @RequestParam("action") String action,
            @RequestBody(required = false) Map<String, Object> metadata) {
        
        if (metadata == null) metadata = new HashMap<>();
        return azureEventGridService.publishStudentEnrollmentEventAsync(studentId, courseId, action, metadata)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/event-grid/course-update")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> publishCourseUpdateEvent(
            @RequestParam("courseId") String courseId,
            @RequestParam("updateType") String updateType,
            @RequestBody Object courseData,
            @RequestParam("updatedBy") String updatedBy) {
        
        return azureEventGridService.publishCourseUpdateEventAsync(courseId, updateType, courseData, updatedBy)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/event-grid/admin-notification")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> publishAdminNotificationEvent(
            @RequestParam("notificationType") String notificationType,
            @RequestParam("message") String message,
            @RequestParam("priority") String priority,
            @RequestBody(required = false) Map<String, Object> details) {
        
        if (details == null) details = new HashMap<>();
        return azureEventGridService.publishAdminNotificationEventAsync(notificationType, message, priority, details)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/event-grid/batch-events")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> publishBatchEvents(
            @RequestBody List<Map<String, Object>> events) {
        
        return azureEventGridService.publishBatchEventsAsync(events)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/event-grid/cloud-event")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> publishCloudEvent(
            @RequestParam("eventType") String eventType,
            @RequestParam("subject") String subject,
            @RequestBody Object data,
            @RequestParam(value = "contentType", defaultValue = "application/json") String contentType) {
        
        return azureEventGridService.publishCloudEventAsync(eventType, subject, data, contentType)
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE SEARCH ENDPOINTS =============

    @PostMapping("/search/index-student")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> indexStudentDocument(
            @RequestParam("studentId") String studentId,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("program") String program,
            @RequestBody List<String> courses,
            @RequestParam(required = false) Map<String, Object> metadata) {
        
        return azureSearchService.indexStudentDocumentAsync(studentId, name, email, program, courses, metadata)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/search/students")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> searchStudents(
            @RequestParam(value = "query", defaultValue = "*") String query,
            @RequestParam(value = "program", required = false) String program,
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
            @RequestParam(value = "top", defaultValue = "10") int top,
            @RequestParam(value = "skip", defaultValue = "0") int skip) {
        
        return azureSearchService.searchStudentsAsync(query, program, includeInactive, top, skip)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/search/suggest")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> suggestSearchTerms(
            @RequestParam("partialQuery") String partialQuery,
            @RequestParam(value = "suggesterName", defaultValue = "student-suggester") String suggesterName) {
        
        return azureSearchService.suggestSearchTermsAsync(partialQuery, suggesterName)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/search/analytics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> performSearchAnalytics(
            @RequestParam(value = "dateRange", defaultValue = "30days") String dateRange) {
        
        return azureSearchService.performSearchAnalyticsAsync(dateRange)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/search/student/{studentId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteStudentDocument(
            @PathVariable String studentId) {
        
        return azureSearchService.deleteStudentDocumentAsync(studentId)
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE APPLICATION INSIGHTS ENDPOINTS =============

    @PostMapping("/app-insights/trace")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createTrace(
            @RequestParam("operationName") String operationName,
            @RequestParam("userId") String userId,
            @RequestParam("duration") long durationMs,
            @RequestBody(required = false) Map<String, String> properties) {
        
        if (properties == null) properties = new HashMap<>();
        return azureApplicationInsightsService.createTraceAsync(operationName, userId, 
                java.time.Duration.ofMillis(durationMs), properties)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/app-insights/metric")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendCustomMetric(
            @RequestParam("metricName") String metricName,
            @RequestParam("value") double value,
            @RequestParam(value = "unit", defaultValue = "Count") String unit,
            @RequestBody(required = false) Map<String, String> dimensions) {
        
        if (dimensions == null) dimensions = new HashMap<>();
        return azureApplicationInsightsService.sendCustomMetricAsync(metricName, value, unit, dimensions)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/app-insights/performance")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> logPerformanceMetrics(
            @RequestParam("endpoint") String endpoint,
            @RequestParam("responseTime") long responseTime,
            @RequestParam("statusCode") int statusCode,
            @RequestParam("httpMethod") String httpMethod) {
        
        return azureApplicationInsightsService.logPerformanceMetricsAsync(endpoint, responseTime, statusCode, httpMethod)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/app-insights/query")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> queryApplicationLogs(
            @RequestParam("query") String query,
            @RequestParam(value = "hours", defaultValue = "24") int hours) {
        
        return azureApplicationInsightsService.queryApplicationLogsAsync(query, hours)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/app-insights/performance-insights")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPerformanceInsights(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        
        return azureApplicationInsightsService.getPerformanceInsightsAsync(days)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/app-insights/alert")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createAlertRule(
            @RequestParam("alertName") String alertName,
            @RequestParam("condition") String condition,
            @RequestParam("threshold") String threshold,
            @RequestBody List<String> actionEmails) {
        
        return azureApplicationInsightsService.createAlertRuleAsync(alertName, condition, threshold, actionEmails)
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE FUNCTIONS ENDPOINTS =============

    @PostMapping("/functions/app")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createFunctionApp(
            @RequestParam("appName") String appName,
            @RequestParam("region") String region,
            @RequestParam("runtime") String runtime) {
        
        return azureFunctionsService.createFunctionAppAsync(appName, region, runtime)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/functions/invoke")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> invokeFunction(
            @RequestParam("functionName") String functionName,
            @RequestBody Map<String, Object> payload) {
        
        return azureFunctionsService.invokeFunctionAsync(functionName, payload)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/functions/process-enrollment")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> processStudentEnrollment(
            @RequestParam("studentId") String studentId,
            @RequestParam("courseId") String courseId,
            @RequestParam("action") String action) {
        
        return azureFunctionsService.processStudentEnrollmentAsync(studentId, courseId, action)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/functions/generate-analytics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateCourseAnalytics(
            @RequestParam("courseId") String courseId,
            @RequestParam("reportType") String reportType,
            @RequestBody Map<String, Object> parameters) {
        
        return azureFunctionsService.generateCourseAnalyticsAsync(courseId, reportType, parameters)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/functions/list")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> listFunctions() {
        return azureFunctionsService.listFunctionsAsync()
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/functions/stats")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getFunctionStats(
            @RequestParam("functionName") String functionName,
            @RequestParam(value = "days", defaultValue = "7") int days) {
        
        return azureFunctionsService.getFunctionStatsAsync(functionName, days)
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE COSMOS DB ENDPOINTS =============

    @PostMapping("/cosmos/initialize")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> initializeDatabase() {
        return azureCosmosDbService.initializeDatabaseAsync()
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/cosmos/student")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createStudent(
            @RequestParam("studentId") String studentId,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("program") String program,
            @RequestBody List<String> courses,
            @RequestParam(required = false) Map<String, Object> metadata) {
        
        return azureCosmosDbService.createStudentAsync(studentId, name, email, program, courses, metadata)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/cosmos/student/{studentId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getStudent(
            @PathVariable String studentId) {
        
        return azureCosmosDbService.getStudentAsync(studentId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/cosmos/students-by-program")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> queryStudentsByProgram(
            @RequestParam("program") String program,
            @RequestParam(value = "maxResults", defaultValue = "50") int maxResults) {
        
        return azureCosmosDbService.queryStudentsByProgramAsync(program, maxResults)
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/cosmos/student/{studentId}/courses")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateStudentCourses(
            @PathVariable String studentId,
            @RequestBody List<String> newCourses) {
        
        return azureCosmosDbService.updateStudentCoursesAsync(studentId, newCourses)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/cosmos/student/{studentId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteStudent(
            @PathVariable String studentId) {
        
        return azureCosmosDbService.deleteStudentAsync(studentId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/cosmos/analytics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getDatabaseAnalytics() {
        return azureCosmosDbService.getDatabaseAnalyticsAsync()
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/cosmos/batch")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> batchOperations(
            @RequestBody List<Map<String, Object>> operations) {
        
        return azureCosmosDbService.batchOperationsAsync(operations)
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE LOGIC APPS ENDPOINTS =============

    @PostMapping("/logic-apps/workflow")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createEnrollmentWorkflow(
            @RequestParam("workflowName") String workflowName,
            @RequestBody Map<String, Object> workflowDefinition) {
        
        return azureLogicAppsService.createEnrollmentWorkflowAsync(workflowName, workflowDefinition)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/logic-apps/execute-enrollment")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeEnrollmentWorkflow(
            @RequestParam("studentId") String studentId,
            @RequestParam("courseId") String courseId,
            @RequestParam("action") String action,
            @RequestBody(required = false) Map<String, Object> metadata) {
        
        if (metadata == null) metadata = new HashMap<>();
        return azureLogicAppsService.executeEnrollmentWorkflowAsync(studentId, courseId, action, metadata)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/logic-apps/execute-analytics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeAnalyticsWorkflow(
            @RequestParam("courseId") String courseId,
            @RequestParam("reportType") String reportType,
            @RequestBody Map<String, Object> parameters) {
        
        return azureLogicAppsService.executeAnalyticsWorkflowAsync(courseId, reportType, parameters)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/logic-apps/execution/{executionId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getWorkflowExecutionStatus(
            @PathVariable String executionId) {
        
        return azureLogicAppsService.getWorkflowExecutionStatusAsync(executionId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/logic-apps/runs")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> listWorkflowRuns(
            @RequestParam("workflowName") String workflowName,
            @RequestParam(value = "days", defaultValue = "7") int days) {
        
        return azureLogicAppsService.listWorkflowRunsAsync(workflowName, days)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/logic-apps/cancel/{executionId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> cancelWorkflowExecution(
            @PathVariable String executionId,
            @RequestParam("reason") String reason) {
        
        return azureLogicAppsService.cancelWorkflowExecutionAsync(executionId, reason)
                .thenApply(ResponseEntity::ok);
    }

    // ============= AZURE ACTIVE DIRECTORY ENDPOINTS =============

    @PostMapping("/ad/user")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createUser(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("password") String password,
            @RequestBody List<String> groups) {
        
        return azureActiveDirectoryService.createUserAsync(username, email, firstName, lastName, password, groups)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/ad/authenticate")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> authenticateUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
        
        return azureActiveDirectoryService.authenticateUserAsync(username, password)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/ad/user/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserInfo(
            @PathVariable String userId) {
        
        return azureActiveDirectoryService.getUserInfoAsync(userId)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/ad/group")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createGroup(
            @RequestParam("groupName") String groupName,
            @RequestParam("description") String description,
            @RequestParam(value = "groupType", defaultValue = "Security") String groupType,
            @RequestBody List<String> members) {
        
        return azureActiveDirectoryService.createGroupAsync(groupName, description, groupType, members)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/ad/group/{groupId}/member/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> addUserToGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        
        return azureActiveDirectoryService.addUserToGroupAsync(userId, groupId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/ad/users")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> listUsers(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "top", defaultValue = "50") int top) {
        
        return azureActiveDirectoryService.listUsersAsync(filter, top)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/ad/user/{userId}/reset-password")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetUserPassword(
            @PathVariable String userId,
            @RequestParam("newPassword") String newPassword,
            @RequestParam(value = "forceChange", defaultValue = "false") boolean forceChangeNextSignIn) {
        
        return azureActiveDirectoryService.resetUserPasswordAsync(userId, newPassword, forceChangeNextSignIn)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/ad/analytics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getDirectoryAnalytics() {
        return azureActiveDirectoryService.getDirectoryAnalyticsAsync()
                .thenApply(ResponseEntity::ok);
    }

    // ============= HEALTH CHECK ENDPOINTS =============

    @GetMapping("/health")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAzureServicesHealth() {
        CompletableFuture<Map<String, Object>> blobHealth = azureBlobStorageService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> cognitiveHealth = azureCognitiveServicesService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> apimHealth = azureApiManagementService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> eventGridHealth = azureEventGridService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> searchHealth = azureSearchService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> appInsightsHealth = azureApplicationInsightsService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> functionsHealth = azureFunctionsService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> cosmosHealth = azureCosmosDbService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> logicAppsHealth = azureLogicAppsService.checkHealthAsync();
        CompletableFuture<Map<String, Object>> adHealth = azureActiveDirectoryService.checkHealthAsync();

        return CompletableFuture.allOf(blobHealth, cognitiveHealth, apimHealth, eventGridHealth, searchHealth,
                appInsightsHealth, functionsHealth, cosmosHealth, logicAppsHealth, adHealth)
                .thenApply(ignored -> {
                    Map<String, Object> overallHealth = new HashMap<>();
                    overallHealth.put("status", "UP");
                    overallHealth.put("timestamp", java.time.LocalDateTime.now());
                    overallHealth.put("platform", "Azure");
                    overallHealth.put("services", Map.of(
                        "blobStorage", blobHealth.join(),
                        "cognitiveServices", cognitiveHealth.join(),
                        "apiManagement", apimHealth.join(),
                        "eventGrid", eventGridHealth.join(),
                        "search", searchHealth.join(),
                        "applicationInsights", appInsightsHealth.join(),
                        "functions", functionsHealth.join(),
                        "cosmosDb", cosmosHealth.join(),
                        "logicApps", logicAppsHealth.join(),
                        "activeDirectory", adHealth.join()
                    ));
                    
                    log.info("Azure services health check completed");
                    return ResponseEntity.ok(overallHealth);
                });
    }

    @GetMapping("/health/{serviceName}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getServiceHealth(@PathVariable String serviceName) {
        CompletableFuture<Map<String, Object>> healthFuture;
        
        switch (serviceName.toLowerCase()) {
            case "blob-storage":
                healthFuture = azureBlobStorageService.checkHealthAsync();
                break;
            case "cognitive-services":
                healthFuture = azureCognitiveServicesService.checkHealthAsync();
                break;
            case "api-management":
                healthFuture = azureApiManagementService.checkHealthAsync();
                break;
            case "event-grid":
                healthFuture = azureEventGridService.checkHealthAsync();
                break;
            case "search":
                healthFuture = azureSearchService.checkHealthAsync();
                break;
            case "application-insights":
                healthFuture = azureApplicationInsightsService.checkHealthAsync();
                break;
            case "functions":
                healthFuture = azureFunctionsService.checkHealthAsync();
                break;
            case "cosmos-db":
                healthFuture = azureCosmosDbService.checkHealthAsync();
                break;
            case "logic-apps":
                healthFuture = azureLogicAppsService.checkHealthAsync();
                break;
            case "active-directory":
                healthFuture = azureActiveDirectoryService.checkHealthAsync();
                break;
            default:
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Unknown service: " + serviceName);
                errorResponse.put("availableServices", List.of("blob-storage", "cognitive-services", 
                    "api-management", "event-grid", "search", "application-insights", 
                    "functions", "cosmos-db", "logic-apps", "active-directory"));
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResponse));
        }
        
        return healthFuture.thenApply(ResponseEntity::ok);
    }
}