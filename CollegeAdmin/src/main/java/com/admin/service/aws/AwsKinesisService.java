package com.admin.service.aws;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.ListStreamsRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;

/**
 * AWS Kinesis Real-Time Data Streaming Service
 * Handles real-time data streams for analytics, monitoring, and event processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsKinesisService {

    private final KinesisClient kinesisClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.kinesis.streams.student-events:college-student-events}")
    private String studentEventsStreamName;

    @Value("${aws.kinesis.streams.audit-logs:college-audit-logs}")
    private String auditLogsStreamName;

    @Value("${aws.kinesis.streams.analytics:college-analytics}")
    private String analyticsStreamName;

    @Value("${aws.kinesis.streams.notifications:college-notifications}")
    private String notificationsStreamName;

    /**
     * Initialize Kinesis streams
     */
    public void initializeStreams() {
        try {
            createStreamIfNotExists(studentEventsStreamName, 2);
            createStreamIfNotExists(auditLogsStreamName, 1);
            createStreamIfNotExists(analyticsStreamName, 3);
            createStreamIfNotExists(notificationsStreamName, 1);
            
            log.info("Kinesis streams initialization completed");
        } catch (Exception e) {
            log.error("Failed to initialize Kinesis streams: {}", e.getMessage());
        }
    }

    /**
     * Send student event to real-time stream
     */
    public CompletableFuture<Boolean> sendStudentEvent(StudentEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StudentEventRecord record = StudentEventRecord.builder()
                        .eventId(UUID.randomUUID().toString())
                        .studentId(event.getStudentId())
                        .eventType(event.getEventType())
                        .timestamp(System.currentTimeMillis())
                        .data(event.getData())
                        .source("college-admin-service")
                        .build();

                String recordData = objectMapper.writeValueAsString(record);

                PutRecordRequest request = PutRecordRequest.builder()
                        .streamName(studentEventsStreamName)
                        .data(SdkBytes.fromString(recordData, StandardCharsets.UTF_8))
                        .partitionKey(event.getStudentId()) // Ensures events for same student go to same shard
                        .build();

                PutRecordResponse response = kinesisClient.putRecord(request);
                
                log.info("Student event sent to Kinesis: {} - Sequence: {}", 
                        event.getEventType(), response.sequenceNumber());
                return true;

            } catch (Exception e) {
                log.error("Failed to send student event to Kinesis: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Send audit log to compliance stream
     */
    public void sendAuditLog(AuditLogEvent auditEvent) {
        try {
            AuditLogRecord record = AuditLogRecord.builder()
                    .auditId(UUID.randomUUID().toString())
                    .userId(auditEvent.getUserId())
                    .action(auditEvent.getAction())
                    .resource(auditEvent.getResource())
                    .timestamp(System.currentTimeMillis())
                    .ipAddress(auditEvent.getIpAddress())
                    .userAgent(auditEvent.getUserAgent())
                    .result(auditEvent.getResult())
                    .details(auditEvent.getDetails())
                    .build();

            String recordData = objectMapper.writeValueAsString(record);

            PutRecordRequest request = PutRecordRequest.builder()
                    .streamName(auditLogsStreamName)
                    .data(SdkBytes.fromString(recordData, StandardCharsets.UTF_8))
                    .partitionKey(auditEvent.getUserId())
                    .build();

            kinesisClient.putRecord(request);
            log.debug("Audit log sent to Kinesis: {} - {}", auditEvent.getAction(), auditEvent.getUserId());

        } catch (Exception e) {
            log.error("Failed to send audit log to Kinesis: {}", e.getMessage());
        }
    }

    /**
     * Send analytics data for real-time processing
     */
    public CompletableFuture<Boolean> sendAnalyticsData(AnalyticsData analyticsData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AnalyticsRecord record = AnalyticsRecord.builder()
                        .recordId(UUID.randomUUID().toString())
                        .metricName(analyticsData.getMetricName())
                        .metricValue(analyticsData.getMetricValue())
                        .dimensions(analyticsData.getDimensions())
                        .timestamp(System.currentTimeMillis())
                        .source(analyticsData.getSource())
                        .build();

                String recordData = objectMapper.writeValueAsString(record);

                PutRecordRequest request = PutRecordRequest.builder()
                        .streamName(analyticsStreamName)
                        .data(SdkBytes.fromString(recordData, StandardCharsets.UTF_8))
                        .partitionKey(analyticsData.getMetricName())
                        .build();

                PutRecordResponse response = kinesisClient.putRecord(request);
                
                log.debug("Analytics data sent to Kinesis: {} = {}", 
                        analyticsData.getMetricName(), analyticsData.getMetricValue());
                return true;

            } catch (Exception e) {
                log.error("Failed to send analytics data to Kinesis: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Batch send multiple records for efficiency
     */
    public CompletableFuture<BatchSendResult> sendBatchRecords(List<Object> records, String streamName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<PutRecordsRequestEntry> entries = records.stream()
                        .map(record -> {
                            try {
                                String recordData = objectMapper.writeValueAsString(record);
                                return PutRecordsRequestEntry.builder()
                                        .data(SdkBytes.fromString(recordData, StandardCharsets.UTF_8))
                                        .partitionKey(UUID.randomUUID().toString())
                                        .build();
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to serialize record", e);
                            }
                        })
                        .collect(Collectors.toList());

                PutRecordsRequest request = PutRecordsRequest.builder()
                        .streamName(streamName)
                        .records(entries)
                        .build();

                PutRecordsResponse response = kinesisClient.putRecords(request);

                int successCount = response.records().size() - response.failedRecordCount();
                
                log.info("Batch sent to Kinesis stream {}: {} successful, {} failed", 
                        streamName, successCount, response.failedRecordCount());

                return BatchSendResult.builder()
                        .totalRecords(entries.size())
                        .successfulRecords(successCount)
                        .failedRecords(response.failedRecordCount())
                        .build();

            } catch (Exception e) {
                log.error("Failed to send batch records to Kinesis: {}", e.getMessage());
                return BatchSendResult.builder()
                        .totalRecords(records.size())
                        .successfulRecords(0)
                        .failedRecords(records.size())
                        .build();
            }
        });
    }

    /**
     * Get stream information and metrics
     */
    public StreamInfo getStreamInfo(String streamName) {
        try {
            DescribeStreamRequest request = DescribeStreamRequest.builder()
                    .streamName(streamName)
                    .build();

            DescribeStreamResponse response = kinesisClient.describeStream(request);
            StreamDescription description = response.streamDescription();

            return StreamInfo.builder()
                    .streamName(description.streamName())
                    .status(description.streamStatus().toString())
                    .shardCount(description.shards().size())
                    .retentionPeriod(description.retentionPeriodHours())
                    .creationTime(description.streamCreationTimestamp())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get stream info for {}: {}", streamName, e.getMessage());
            return null;
        }
    }

    /**
     * List all Kinesis streams
     */
    public List<String> listStreams() {
        try {
            ListStreamsRequest request = ListStreamsRequest.builder().build();
            ListStreamsResponse response = kinesisClient.listStreams(request);
            
            log.info("Available Kinesis streams: {}", response.streamNames());
            return response.streamNames();

        } catch (Exception e) {
            log.error("Failed to list Kinesis streams: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Monitor stream for real-time processing metrics
     */
    public void monitorStreamMetrics(String streamName) {
        CompletableFuture.runAsync(() -> {
            try {
                StreamInfo info = getStreamInfo(streamName);
                if (info != null) {
                    log.info("Stream Metrics for {}: Status={}, Shards={}, Retention={}h", 
                            streamName, info.getStatus(), info.getShardCount(), info.getRetentionPeriod());
                }
                
                // Additional monitoring logic for CloudWatch metrics
                // Implementation would integrate with CloudWatch API
                
            } catch (Exception e) {
                log.error("Failed to monitor stream metrics: {}", e.getMessage());
            }
        });
    }

    // Helper methods
    private void createStreamIfNotExists(String streamName, int shardCount) {
        try {
            DescribeStreamRequest describeRequest = DescribeStreamRequest.builder()
                    .streamName(streamName)
                    .build();
            
            kinesisClient.describeStream(describeRequest);
            log.info("Kinesis stream {} already exists", streamName);

        } catch (ResourceNotFoundException e) {
            // Stream doesn't exist, create it
            try {
                CreateStreamRequest createRequest = CreateStreamRequest.builder()
                        .streamName(streamName)
                        .shardCount(shardCount)
                        .build();

                kinesisClient.createStream(createRequest);
                log.info("Created Kinesis stream: {} with {} shards", streamName, shardCount);

                // Wait for stream to be active
                waitForStreamToBecomeActive(streamName);

            } catch (Exception createException) {
                log.error("Failed to create Kinesis stream {}: {}", streamName, createException.getMessage());
            }
        }
    }

    private void waitForStreamToBecomeActive(String streamName) {
        try {
            Thread.sleep(10000); // Simple wait - in production, use proper polling
            log.info("Stream {} should be active now", streamName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get Kinesis service health status
     */
    public CompletableFuture<Map<String, Object>> getKinesisHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test connection by listing streams
                ListStreamsRequest request = ListStreamsRequest.builder()
                        .limit(1)
                        .build();
                
                kinesisClient.listStreams(request);

                return Map.of(
                        "status", "UP",
                        "service", "AWS Kinesis",
                        "timestamp", java.time.Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("Kinesis health check failed", e);
                return Map.of(
                        "status", "DOWN",
                        "error", e.getMessage(),
                        "timestamp", java.time.Instant.now().toString()
                );
            }
        });
    }

    // DTOs for Kinesis records
    @lombok.Data
    @lombok.Builder
    public static class StudentEvent {
        private String studentId;
        private String eventType;
        private Map<String, Object> data;
    }

    @lombok.Data
    @lombok.Builder
    public static class StudentEventRecord {
        private String eventId;
        private String studentId;
        private String eventType;
        private long timestamp;
        private Map<String, Object> data;
        private String source;
    }

    @lombok.Data
    @lombok.Builder
    public static class AuditLogEvent {
        private String userId;
        private String action;
        private String resource;
        private String ipAddress;
        private String userAgent;
        private String result;
        private Map<String, Object> details;
    }

    @lombok.Data
    @lombok.Builder
    public static class AuditLogRecord {
        private String auditId;
        private String userId;
        private String action;
        private String resource;
        private long timestamp;
        private String ipAddress;
        private String userAgent;
        private String result;
        private Map<String, Object> details;
    }

    @lombok.Data
    @lombok.Builder
    public static class AnalyticsData {
        private String metricName;
        private double metricValue;
        private Map<String, String> dimensions;
        private String source;
    }

    @lombok.Data
    @lombok.Builder
    public static class AnalyticsRecord {
        private String recordId;
        private String metricName;
        private double metricValue;
        private Map<String, String> dimensions;
        private long timestamp;
        private String source;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchSendResult {
        private int totalRecords;
        private int successfulRecords;
        private int failedRecords;
    }

    @lombok.Data
    @lombok.Builder
    public static class StreamInfo {
        private String streamName;
        private String status;
        private int shardCount;
        private int retentionPeriod;
        private java.time.Instant creationTime;
    }
}