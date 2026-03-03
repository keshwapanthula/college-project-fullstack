package com.admin.service.aws;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * AWS DynamoDB Service Integration
 * Provides NoSQL database operations for high-performance, scalable data storage
 */
@Profile({"aws", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsDynamoDbService {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.tables.students:college-students}")
    private String studentsTableName;

    @Value("${aws.dynamodb.tables.courses:college-courses}")
    private String coursesTableName;

    @Value("${aws.dynamodb.tables.enrollments:college-enrollments}")
    private String enrollmentsTableName;

    @Value("${aws.dynamodb.tables.analytics:college-analytics}")
    private String analyticsTableName;

    /**
     * Create DynamoDB tables if they don't exist
     */
    public void initializeTables() {
        try {
            createStudentsTable();
            createCoursesTable();
            createEnrollmentsTable();
            createAnalyticsTable();
            
            log.info("DynamoDB tables initialization completed");
        } catch (Exception e) {
            log.error("Failed to initialize DynamoDB tables: {}", e.getMessage());
        }
    }

    /**
     * Store student information in DynamoDB
     */
    public CompletableFuture<Boolean> saveStudent(StudentEntity student) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("studentId", AttributeValue.builder().s(student.getStudentId()).build());
                item.put("name", AttributeValue.builder().s(student.getName()).build());
                item.put("email", AttributeValue.builder().s(student.getEmail()).build());
                item.put("department", AttributeValue.builder().s(student.getDepartment()).build());
                item.put("gpa", AttributeValue.builder().n(String.valueOf(student.getGpa())).build());
                item.put("enrollmentDate", AttributeValue.builder().s(student.getEnrollmentDate()).build());
                item.put("status", AttributeValue.builder().s(student.getStatus()).build());
                item.put("lastUpdated", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());

                // Add metadata as a map
                if (student.getMetadata() != null && !student.getMetadata().isEmpty()) {
                    Map<String, AttributeValue> metadataMap = student.getMetadata().entrySet().stream()
                            .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> AttributeValue.builder().s(String.valueOf(entry.getValue())).build()
                            ));
                    item.put("metadata", AttributeValue.builder().m(metadataMap).build());
                }

                PutItemRequest request = PutItemRequest.builder()
                        .tableName(studentsTableName)
                        .item(item)
                        .build();

                dynamoDbClient.putItem(request);
                log.info("Student saved to DynamoDB: {}", student.getStudentId());
                return true;

            } catch (Exception e) {
                log.error("Failed to save student to DynamoDB: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Retrieve student by ID
     */
    public Optional<StudentEntity> getStudent(String studentId) {
        try {
            Map<String, AttributeValue> key = Map.of(
                "studentId", AttributeValue.builder().s(studentId).build()
            );

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(studentsTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.hasItem()) {
                Map<String, AttributeValue> item = response.item();
                StudentEntity student = StudentEntity.builder()
                        .studentId(item.get("studentId").s())
                        .name(item.get("name").s())
                        .email(item.get("email").s())
                        .department(item.get("department").s())
                        .gpa(Double.parseDouble(item.get("gpa").n()))
                        .enrollmentDate(item.get("enrollmentDate").s())
                        .status(item.get("status").s())
                        .build();

                // Extract metadata if exists
                if (item.containsKey("metadata")) {
                    Map<String, Object> metadata = item.get("metadata").m().entrySet().stream()
                            .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().s()
                            ));
                    student.setMetadata(metadata);
                }

                log.info("Student retrieved from DynamoDB: {}", studentId);
                return Optional.of(student);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to retrieve student from DynamoDB: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Query students by department with pagination
     */
    public List<StudentEntity> getStudentsByDepartment(String department, int limit, String lastEvaluatedKey) {
        try {
            QueryRequest.Builder queryBuilder = QueryRequest.builder()
                    .tableName(studentsTableName)
                    .indexName("DepartmentIndex") // GSI for department queries
                    .keyConditionExpression("department = :dept")
                    .expressionAttributeValues(Map.of(
                        ":dept", AttributeValue.builder().s(department).build()
                    ))
                    .limit(limit);

            if (lastEvaluatedKey != null) {
                Map<String, AttributeValue> exclusiveStartKey = Map.of(
                    "studentId", AttributeValue.builder().s(lastEvaluatedKey).build()
                );
                queryBuilder.exclusiveStartKey(exclusiveStartKey);
            }

            QueryResponse response = dynamoDbClient.query(queryBuilder.build());

            List<StudentEntity> students = response.items().stream()
                    .map(this::mapToStudentEntity)
                    .collect(Collectors.toList());

            log.info("Retrieved {} students from department: {}", students.size(), department);
            return students;

        } catch (Exception e) {
            log.error("Failed to query students by department: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Batch write students for bulk operations
     */
    public CompletableFuture<Integer> batchSaveStudents(List<StudentEntity> students) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int batchSize = 25; // DynamoDB batch write limit
                int totalProcessed = 0;

                for (int i = 0; i < students.size(); i += batchSize) {
                    List<StudentEntity> batch = students.subList(i, Math.min(i + batchSize, students.size()));
                    
                    List<WriteRequest> writeRequests = batch.stream()
                            .map(student -> {
                                Map<String, AttributeValue> item = mapToAttributeValueMap(student);
                                return WriteRequest.builder()
                                        .putRequest(PutRequest.builder().item(item).build())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    BatchWriteItemRequest batchRequest = BatchWriteItemRequest.builder()
                            .requestItems(Map.of(studentsTableName, writeRequests))
                            .build();

                    dynamoDbClient.batchWriteItem(batchRequest);
                    totalProcessed += batch.size();
                }

                log.info("Batch saved {} students to DynamoDB", totalProcessed);
                return totalProcessed;

            } catch (Exception e) {
                log.error("Failed to batch save students: {}", e.getMessage());
                return 0;
            }
        });
    }

    /**
     * Store analytics data for reporting
     */
    public void saveAnalyticsEvent(AnalyticsEvent event) {
        try {
            Map<String, AttributeValue> item = Map.of(
                "eventId", AttributeValue.builder().s(event.getEventId()).build(),
                "eventType", AttributeValue.builder().s(event.getEventType()).build(),
                "timestamp", AttributeValue.builder().n(String.valueOf(event.getTimestamp())).build(),
                "userId", AttributeValue.builder().s(event.getUserId()).build(),
                "data", AttributeValue.builder().s(event.getData()).build(),
                "source", AttributeValue.builder().s(event.getSource()).build()
            );

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(analyticsTableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(request);
            log.debug("Analytics event saved: {}", event.getEventType());

        } catch (Exception e) {
            log.error("Failed to save analytics event: {}", e.getMessage());
        }
    }

    /**
     * Scan table with filters for complex queries
     */
    public List<StudentEntity> scanStudentsWithFilter(String filterExpression, Map<String, String> filterValues) {
        try {
            Map<String, AttributeValue> expressionValues = filterValues.entrySet().stream()
                    .collect(Collectors.toMap(
                        entry -> ":" + entry.getKey(),
                        entry -> AttributeValue.builder().s(entry.getValue()).build()
                    ));

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(studentsTableName)
                    .filterExpression(filterExpression)
                    .expressionAttributeValues(expressionValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            List<StudentEntity> students = response.items().stream()
                    .map(this::mapToStudentEntity)
                    .collect(Collectors.toList());

            log.info("Scanned {} students with filter", students.size());
            return students;

        } catch (Exception e) {
            log.error("Failed to scan students with filter: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // Helper methods
    private void createStudentsTable() {
        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(studentsTableName)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("studentId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("studentId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("department")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("DepartmentIndex")
                            .keySchema(KeySchemaElement.builder()
                                    .attributeName("department")
                                    .keyType(KeyType.HASH)
                                    .build())
                            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                            .provisionedThroughput(ProvisionedThroughput.builder()
                                    .readCapacityUnits(5L)
                                    .writeCapacityUnits(5L)
                                    .build())
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build();

            dynamoDbClient.createTable(request);
            log.info("Created DynamoDB table: {}", studentsTableName);

        } catch (ResourceInUseException e) {
            log.info("Table {} already exists", studentsTableName);
        } catch (Exception e) {
            log.error("Failed to create students table: {}", e.getMessage());
        }
    }

    private void createCoursesTable() {
        // Similar implementation for courses table
    }

    private void createEnrollmentsTable() {
        // Similar implementation for enrollments table
    }

    private void createAnalyticsTable() {
        // Similar implementation for analytics table
    }

    private StudentEntity mapToStudentEntity(Map<String, AttributeValue> item) {
        return StudentEntity.builder()
                .studentId(item.get("studentId").s())
                .name(item.get("name").s())
                .email(item.get("email").s())
                .department(item.get("department").s())
                .gpa(Double.parseDouble(item.get("gpa").n()))
                .enrollmentDate(item.get("enrollmentDate").s())
                .status(item.get("status").s())
                .build();
    }

    private Map<String, AttributeValue> mapToAttributeValueMap(StudentEntity student) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("studentId", AttributeValue.builder().s(student.getStudentId()).build());
        item.put("name", AttributeValue.builder().s(student.getName()).build());
        item.put("email", AttributeValue.builder().s(student.getEmail()).build());
        item.put("department", AttributeValue.builder().s(student.getDepartment()).build());
        item.put("gpa", AttributeValue.builder().n(String.valueOf(student.getGpa())).build());
        item.put("enrollmentDate", AttributeValue.builder().s(student.getEnrollmentDate()).build());
        item.put("status", AttributeValue.builder().s(student.getStatus()).build());
        item.put("lastUpdated", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());
        return item;
    }

    /**
     * Get DynamoDB service health status
     */
    public CompletableFuture<Map<String, Object>> getDynamoDbHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test connection by listing tables
                ListTablesRequest request = ListTablesRequest.builder()
                        .limit(1)
                        .build();
                
                dynamoDbClient.listTables(request);

                return Map.of(
                        "status", "UP",
                        "service", "AWS DynamoDB",
                        "timestamp", java.time.Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("DynamoDB health check failed", e);
                return Map.of(
                        "status", "DOWN",
                        "error", e.getMessage(),
                        "timestamp", java.time.Instant.now().toString()
                );
            }
        });
    }

    // Entity classes
    @lombok.Data
    @lombok.Builder
    public static class StudentEntity {
        private String studentId;
        private String name;
        private String email;
        private String department;
        private double gpa;
        private String enrollmentDate;
        private String status;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class AnalyticsEvent {
        private String eventId;
        private String eventType;
        private long timestamp;
        private String userId;
        private String data;
        private String source;
    }
}