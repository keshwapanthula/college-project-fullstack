package com.admin.service.azure;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.ThroughputProperties;
import com.azure.cosmos.util.CosmosPagedIterable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure Cosmos DB Service
 * Provides NoSQL database capabilities equivalent to AWS DynamoDB
 * including document operations, queries, and analytics
 */
@Profile({"azure", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureCosmosDbService {

    private final CosmosClient cosmosClient;

    @Value("${azure.cosmosdb.database-name:college-admin-db}")
    private String databaseName;

    @Value("${azure.cosmosdb.container-name:students}")
    private String containerName;

    /**
     * Create database and container if they don't exist
     */
    public CompletableFuture<Map<String, Object>> initializeDatabaseAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Initializing Cosmos DB database and container");

                // Create database if it doesn't exist
                cosmosClient.createDatabaseIfNotExists(databaseName);
                CosmosDatabase database = cosmosClient.getDatabase(databaseName);

                // Create container if it doesn't exist
                database.createContainerIfNotExists(
                    containerName, 
                    "/studentId", 
                    ThroughputProperties.createManualThroughput(400)
                );
                CosmosContainer container = database.getContainer(containerName);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("databaseName", databaseName);
                result.put("containerName", containerName);
                result.put("partitionKey", "/studentId");
                result.put("throughput", 400);
                result.put("initializedTime", LocalDateTime.now());

                log.info("Cosmos DB database and container initialized successfully");
                return result;

            } catch (Exception e) {
                log.error("Failed to initialize Cosmos DB: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Create or update student document
     */
    public CompletableFuture<Map<String, Object>> createStudentAsync(String studentId, String name, String email, 
                                                                    String program, List<String> courses, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating/updating student document: {} - {}", studentId, name);

                CosmosContainer container = cosmosClient.getDatabase(databaseName).getContainer(containerName);

                Map<String, Object> studentDoc = new HashMap<>();
                studentDoc.put("id", studentId);
                studentDoc.put("studentId", studentId);
                studentDoc.put("name", name);
                studentDoc.put("email", email);
                studentDoc.put("program", program);
                studentDoc.put("courses", courses);
                studentDoc.put("enrollmentDate", LocalDateTime.now().toString());
                studentDoc.put("isActive", true);
                studentDoc.put("lastModified", LocalDateTime.now().toString());
                
                if (metadata != null) {
                    studentDoc.putAll(metadata);
                }

                CosmosItemResponse<Object> response = container.upsertItem(studentDoc);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("studentId", studentId);
                result.put("statusCode", response.getStatusCode());
                result.put("requestCharge", response.getRequestCharge());
                result.put("etag", response.getETag());
                result.put("activityId", response.getActivityId());
                result.put("creationTime", LocalDateTime.now());

                log.info("Student document created/updated successfully: {}", studentId);
                return result;

            } catch (Exception e) {
                log.error("Failed to create student document: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Get student document by ID
     */
    public CompletableFuture<Map<String, Object>> getStudentAsync(String studentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Getting student document: {}", studentId);

                CosmosContainer container = cosmosClient.getDatabase(databaseName).getContainer(containerName);

                CosmosItemResponse<Object> response = container.readItem(
                    studentId, 
                    new PartitionKey(studentId), 
                    Object.class
                );

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("student", response.getItem());
                result.put("statusCode", response.getStatusCode());
                result.put("requestCharge", response.getRequestCharge());
                result.put("etag", response.getETag());
                result.put("retrievalTime", LocalDateTime.now());

                log.info("Student document retrieved successfully: {}", studentId);
                return result;

            } catch (CosmosException e) {
                if (e.getStatusCode() == 404) {
                    log.warn("Student document not found: {}", studentId);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("error", "Student not found");
                    result.put("statusCode", 404);
                    return result;
                }
                throw e;
            } catch (Exception e) {
                log.error("Failed to get student document: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Query students by program
     */
    public CompletableFuture<Map<String, Object>> queryStudentsByProgramAsync(String program, int maxResults) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Querying students by program: {}", program);

                CosmosContainer container = cosmosClient.getDatabase(databaseName).getContainer(containerName);

                String sql = "SELECT * FROM c WHERE c.program = @program AND c.isActive = true";
                CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();

                CosmosPagedIterable<Object> queryResponse = container.queryItems(
                    sql,
                    options,
                    Object.class
                );

                List<Object> students = queryResponse.stream()
                    .limit(maxResults)
                    .collect(Collectors.toList());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("program", program);
                result.put("studentCount", students.size());
                result.put("students", students);
                result.put("queryTime", LocalDateTime.now());

                log.info("Successfully queried {} students for program: {}", students.size(), program);
                return result;

            } catch (Exception e) {
                log.error("Failed to query students by program: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Update student courses
     */
    public CompletableFuture<Map<String, Object>> updateStudentCoursesAsync(String studentId, List<String> newCourses) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Updating courses for student: {}", studentId);

                CosmosContainer container = cosmosClient.getDatabase(databaseName).getContainer(containerName);

                // First, get the existing document
                CosmosItemResponse<Object> getResponse = container.readItem(
                    studentId,
                    new PartitionKey(studentId),
                    Object.class
                );

                @SuppressWarnings("unchecked")
                Map<String, Object> student = (Map<String, Object>) getResponse.getItem();
                student.put("courses", newCourses);
                student.put("lastModified", LocalDateTime.now().toString());

                CosmosItemResponse<Object> updateResponse = container.upsertItem(student);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("studentId", studentId);
                result.put("updatedCourses", newCourses);
                result.put("statusCode", updateResponse.getStatusCode());
                result.put("requestCharge", updateResponse.getRequestCharge());
                result.put("updateTime", LocalDateTime.now());

                log.info("Student courses updated successfully: {}", studentId);
                return result;

            } catch (Exception e) {
                log.error("Failed to update student courses: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Delete student document
     */
    public CompletableFuture<Map<String, Object>> deleteStudentAsync(String studentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Deleting student document: {}", studentId);

                CosmosContainer container = cosmosClient.getDatabase(databaseName).getContainer(containerName);

                CosmosItemResponse<Object> response = container.deleteItem(
                    studentId,
                    new PartitionKey(studentId),
                    new CosmosItemRequestOptions()
                );

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("studentId", studentId);
                result.put("statusCode", response.getStatusCode());
                result.put("requestCharge", response.getRequestCharge());
                result.put("deletionTime", LocalDateTime.now());

                log.info("Student document deleted successfully: {}", studentId);
                return result;

            } catch (Exception e) {
                log.error("Failed to delete student document: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Get database analytics and statistics
     */
    public CompletableFuture<Map<String, Object>> getDatabaseAnalyticsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Getting Cosmos DB analytics and statistics");

                CosmosContainer container = cosmosClient.getDatabase(databaseName).getContainer(containerName);

                // Count total documents
                String countSql = "SELECT VALUE COUNT(1) FROM c";
                CosmosPagedIterable<Integer> countResult = container.queryItems(
                    countSql,
                    new CosmosQueryRequestOptions(),
                    Integer.class
                );
                int totalDocuments = countResult.iterator().next();

                // Count by program
                String programSql = "SELECT c.program, COUNT(1) as count FROM c WHERE c.isActive = true GROUP BY c.program";
                CosmosPagedIterable<Object> programResult = container.queryItems(
                    programSql,
                    new CosmosQueryRequestOptions(),
                    Object.class
                );
                List<Object> programStats = programResult.stream().collect(Collectors.toList());

                Map<String, Object> analytics = new HashMap<>();
                analytics.put("totalDocuments", totalDocuments);
                analytics.put("programDistribution", programStats);
                analytics.put("databaseName", databaseName);
                analytics.put("containerName", containerName);

                // Mock additional analytics
                analytics.put("averageDocumentSize", Math.random() * 5 + 2); // KB
                analytics.put("indexedProperties", Arrays.asList("studentId", "program", "email", "isActive"));
                analytics.put("throughputUsage", Map.of(
                    "current", Math.random() * 200 + 100,
                    "maximum", 400
                ));

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("analytics", analytics);
                result.put("analysisTime", LocalDateTime.now());

                log.info("Database analytics retrieved successfully");
                return result;

            } catch (Exception e) {
                log.error("Failed to get database analytics: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Batch operations for multiple students
     */
    public CompletableFuture<Map<String, Object>> batchOperationsAsync(List<Map<String, Object>> operations) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Performing batch operations: {} operations", operations.size());

                CosmosContainer container = cosmosClient.getDatabase(databaseName).getContainer(containerName);
                List<Map<String, Object>> results = new ArrayList<>();
                int successCount = 0;
                int failureCount = 0;

                for (Map<String, Object> operation : operations) {
                    try {
                        String operationType = (String) operation.get("operation");
                        Map<String, Object> data = (Map<String, Object>) operation.get("data");
                        
                        Map<String, Object> operationResult = new HashMap<>();
                        operationResult.put("operation", operationType);
                        operationResult.put("studentId", data.get("studentId"));

                        switch (operationType.toLowerCase()) {
                            case "create":
                            case "update":
                                CosmosItemResponse<Object> upsertResponse = container.upsertItem(data);
                                operationResult.put("success", true);
                                operationResult.put("statusCode", upsertResponse.getStatusCode());
                                operationResult.put("requestCharge", upsertResponse.getRequestCharge());
                                successCount++;
                                break;

                            case "delete":
                                CosmosItemResponse<Object> deleteResponse = container.deleteItem(
                                    (String) data.get("studentId"),
                                    new PartitionKey((String) data.get("studentId")),
                                    new CosmosItemRequestOptions()
                                );
                                operationResult.put("success", true);
                                operationResult.put("statusCode", deleteResponse.getStatusCode());
                                operationResult.put("requestCharge", deleteResponse.getRequestCharge());
                                successCount++;
                                break;

                            default:
                                operationResult.put("success", false);
                                operationResult.put("error", "Unknown operation type: " + operationType);
                                failureCount++;
                        }

                        results.add(operationResult);

                    } catch (Exception e) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", e.getMessage());
                        errorResult.put("operation", operation.get("operation"));
                        results.add(errorResult);
                        failureCount++;
                    }
                }

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("totalOperations", operations.size());
                result.put("successfulOperations", successCount);
                result.put("failedOperations", failureCount);
                result.put("results", results);
                result.put("batchTime", LocalDateTime.now());

                log.info("Batch operations completed: {}/{} successful", successCount, operations.size());
                return result;

            } catch (Exception e) {
                log.error("Failed to perform batch operations: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Health check for Azure Cosmos DB service
     */
    public CompletableFuture<Map<String, Object>> checkHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> health = new HashMap<>();
            try {
                // Test connectivity with a simple query
                CosmosContainer container = cosmosClient.getDatabase(databaseName).getContainer(containerName);
                String testSql = "SELECT VALUE COUNT(1) FROM c";
                CosmosPagedIterable<Integer> result = container.queryItems(
                    testSql,
                    new CosmosQueryRequestOptions(),
                    Integer.class
                );
                int documentCount = result.iterator().hasNext() ? result.iterator().next() : 0;

                health.put("service", "Azure Cosmos DB");
                health.put("status", "UP");
                health.put("timestamp", LocalDateTime.now());
                health.put("databaseName", databaseName);
                health.put("containerName", containerName);
                health.put("documentCount", documentCount);

                log.debug("Azure Cosmos DB health check passed");

            } catch (Exception e) {
                health.put("service", "Azure Cosmos DB");
                health.put("status", "DOWN");
                health.put("error", e.getMessage());
                health.put("timestamp", LocalDateTime.now());

                log.error("Azure Cosmos DB health check failed: {}", e.getMessage());
            }
            return health;
        });
    }
}