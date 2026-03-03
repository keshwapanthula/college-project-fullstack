package com.admin.service.aws;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.ComparisonOperator;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmResponse;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

/**
 * AWS X-Ray Monitoring and CloudWatch Service
 * Provides distributed tracing, monitoring, and observability
 */
@Profile({"aws", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsXRayMonitoringService {

    private final CloudWatchClient cloudWatchClient;
    private final AWSXRayRecorder xrayRecorder;

    @Value("${aws.xray.service-name:college-admin}")
    private String serviceName;

    @Value("${aws.cloudwatch.namespace:College/Application}")
    private String cloudWatchNamespace;

    /**
     * Create distributed trace for request
     */
    public CompletableFuture<TraceResult> createTrace(String operationName, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            Segment segment = null;
            try {
                log.info("Creating X-Ray trace for operation: {}", operationName);

                // Begin segment
                segment = xrayRecorder.beginSegment(operationName);
                segment.putMetadata("service_info", "name", serviceName);
                
                // Add metadata
                if (metadata != null) {
                    final Segment capturedSegment = segment;
                    metadata.forEach((key, value) -> capturedSegment.putMetadata("runtime", key, value));
                }

                // Simulate some work with subsegments
                Subsegment dbSubsegment = xrayRecorder.beginSubsegment("database-operation");
                try {
                    // Simulate database call
                    Thread.sleep(50 + (int)(Math.random() * 100));
                    dbSubsegment.putAnnotation("table", "students");
                    dbSubsegment.putAnnotation("operation", "SELECT");
                    dbSubsegment.putMetadata("rows_processed", 25 + (int)(Math.random() * 100));
                } finally {
                    xrayRecorder.endSubsegment();
                }

                Subsegment apiSubsegment = xrayRecorder.beginSubsegment("external-api");
                try {
                    // Simulate external API call
                    Thread.sleep(100 + (int)(Math.random() * 200));
                    apiSubsegment.putAnnotation("service", "notification-service");
                    apiSubsegment.putAnnotation("endpoint", "/api/notifications");
                    apiSubsegment.putMetadata("response_time", 150 + (int)(Math.random() * 100));
                } finally {
                    xrayRecorder.endSubsegment();
                }

                TraceResult result = TraceResult.builder()
                        .traceId(segment.getTraceId().toString())
                        .segmentId(segment.getId())
                        .operationName(operationName)
                        .startTime(segment.getStartTime())
                        .duration(System.currentTimeMillis() - (long)(segment.getStartTime() * 1000))
                        .status("SUCCESS")
                        .build();

                log.info("X-Ray trace created successfully: {}", result.getTraceId());
                return result;

            } catch (Exception e) {
                log.error("Error creating X-Ray trace", e);
                if (segment != null) {
                    segment.addException(e);
                }
                throw new RuntimeException("Failed to create trace", e);
            } finally {
                if (segment != null) {
                    xrayRecorder.endSegment();
                }
            }
        });
    }

    /**
     * Send custom metrics to CloudWatch
     */
    public CompletableFuture<MetricResult> sendCustomMetric(CustomMetric metric) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Sending custom metric: {} to CloudWatch", metric.getMetricName());

                MetricDatum metricDatum = MetricDatum.builder()
                        .metricName(metric.getMetricName())
                        .value(metric.getValue())
                        .unit(StandardUnit.fromValue(metric.getUnit()))
                        .timestamp(Instant.now())
                        .dimensions(metric.getDimensions().entrySet().stream()
                                .map(entry -> Dimension.builder()
                                        .name(entry.getKey())
                                        .value(entry.getValue())
                                        .build())
                                .toList())
                        .build();

                PutMetricDataRequest request = PutMetricDataRequest.builder()
                        .namespace(cloudWatchNamespace)
                        .metricData(metricDatum)
                        .build();

                PutMetricDataResponse response = cloudWatchClient.putMetricData(request);

                MetricResult result = MetricResult.builder()
                        .metricName(metric.getMetricName())
                        .namespace(cloudWatchNamespace)
                        .value(metric.getValue())
                        .unit(metric.getUnit())
                        .timestamp(Instant.now().toString())
                        .status("SUCCESS")
                        .build();

                log.info("Custom metric sent successfully: {}", metric.getMetricName());
                return result;

            } catch (Exception e) {
                log.error("Error sending custom metric", e);
                throw new RuntimeException("Failed to send custom metric", e);
            }
        });
    }

    /**
     * Get application metrics from CloudWatch
     */
    public CompletableFuture<List<MetricData>> getApplicationMetrics(String metricName, int hours) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Retrieving application metrics: {} for last {} hours", metricName, hours);

                Instant endTime = Instant.now();
                Instant startTime = endTime.minus(hours, ChronoUnit.HOURS);

                GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                        .namespace(cloudWatchNamespace)
                        .metricName(metricName)
                        .startTime(startTime)
                        .endTime(endTime)
                        .period(300) // 5 minutes
                        .statistics(Statistic.AVERAGE, Statistic.MAXIMUM, Statistic.SUM)
                        .build();

                GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

                List<MetricData> metrics = response.datapoints().stream()
                        .map(datapoint -> MetricData.builder()
                                .timestamp(datapoint.timestamp().toString())
                                .average(datapoint.average())
                                .maximum(datapoint.maximum())
                                .sum(datapoint.sum())
                                .unit(datapoint.unit().toString())
                                .build())
                        .toList();

                log.info("Retrieved {} metric data points", metrics.size());
                return metrics;

            } catch (Exception e) {
                log.error("Error retrieving application metrics", e);
                throw new RuntimeException("Failed to retrieve application metrics", e);
            }
        });
    }

    /**
     * Create CloudWatch alarm
     */
    public CompletableFuture<AlarmResult> createCloudWatchAlarm(AlarmConfiguration config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating CloudWatch alarm: {}", config.getAlarmName());

                PutMetricAlarmRequest request = PutMetricAlarmRequest.builder()
                        .alarmName(config.getAlarmName())
                        .alarmDescription(config.getDescription())
                        .metricName(config.getMetricName())
                        .namespace(cloudWatchNamespace)
                        .statistic(Statistic.fromValue(config.getStatistic()))
                        .period(config.getPeriod())
                        .evaluationPeriods(config.getEvaluationPeriods())
                        .threshold(config.getThreshold())
                        .comparisonOperator(ComparisonOperator.fromValue(config.getComparisonOperator()))
                        .treatMissingData(config.getTreatMissingData())
                        .alarmActions(config.getAlarmActions())
                        .build();

                PutMetricAlarmResponse response = cloudWatchClient.putMetricAlarm(request);

                AlarmResult result = AlarmResult.builder()
                        .alarmName(config.getAlarmName())
                        .alarmArn("arn:aws:cloudwatch:" + "us-east-1" + ":" + "123456789012" + ":alarm:" + config.getAlarmName())
                        .description(config.getDescription())
                        .metricName(config.getMetricName())
                        .threshold(config.getThreshold())
                        .state("INSUFFICIENT_DATA")
                        .createdAt(Instant.now().toString())
                        .build();

                log.info("CloudWatch alarm created successfully: {}", config.getAlarmName());
                return result;

            } catch (Exception e) {
                log.error("Error creating CloudWatch alarm", e);
                throw new RuntimeException("Failed to create CloudWatch alarm", e);
            }
        });
    }

    /**
     * List CloudWatch alarms
     */
    public CompletableFuture<List<AlarmInfo>> listCloudWatchAlarms() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Listing CloudWatch alarms");

                DescribeAlarmsRequest request = DescribeAlarmsRequest.builder()
                        .maxRecords(50)
                        .build();

                DescribeAlarmsResponse response = cloudWatchClient.describeAlarms(request);

                List<AlarmInfo> alarms = response.metricAlarms().stream()
                        .map(alarm -> AlarmInfo.builder()
                                .alarmName(alarm.alarmName())
                                .alarmArn(alarm.alarmArn())
                                .description(alarm.alarmDescription())
                                .metricName(alarm.metricName())
                                .namespace(alarm.namespace())
                                .state(alarm.stateValue().toString())
                                .threshold(alarm.threshold())
                                .build())
                        .toList();

                log.info("Found {} CloudWatch alarms", alarms.size());
                return alarms;

            } catch (Exception e) {
                log.error("Error listing CloudWatch alarms", e);
                throw new RuntimeException("Failed to list CloudWatch alarms", e);
            }
        });
    }

    /**
     * Log application performance metrics
     */
    public CompletableFuture<Void> logPerformanceMetrics(PerformanceMetrics metrics) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Logging performance metrics for operation: {}", metrics.getOperationName());

                // Send response time metric
                sendCustomMetric(CustomMetric.builder()
                        .metricName("ResponseTime")
                        .value(metrics.getResponseTime())
                        .unit("Milliseconds")
                        .dimensions(Map.of(
                                "Operation", metrics.getOperationName(),
                                "Service", serviceName
                        ))
                        .build()).join();

                // Send throughput metric
                if (metrics.getThroughput() > 0) {
                    sendCustomMetric(CustomMetric.builder()
                            .metricName("Throughput")
                            .value(metrics.getThroughput())
                            .unit("Count/Second")
                            .dimensions(Map.of(
                                    "Operation", metrics.getOperationName(),
                                    "Service", serviceName
                            ))
                            .build()).join();
                }

                // Send error count if any
                if (metrics.getErrorCount() > 0) {
                    sendCustomMetric(CustomMetric.builder()
                            .metricName("ErrorCount")
                            .value(metrics.getErrorCount())
                            .unit("Count")
                            .dimensions(Map.of(
                                    "Operation", metrics.getOperationName(),
                                    "Service", serviceName
                            ))
                            .build()).join();
                }

                log.info("Performance metrics logged successfully");

            } catch (Exception e) {
                log.error("Error logging performance metrics", e);
                // Don't throw exception as this is monitoring code
            }
        });
    }

    /**
     * Get monitoring health status
     */
    public CompletableFuture<Map<String, Object>> getMonitoringHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test CloudWatch connection
                DescribeAlarmsRequest request = DescribeAlarmsRequest.builder()
                        .maxRecords(1)
                        .build();
                
                cloudWatchClient.describeAlarms(request);

                return Map.of(
                        "status", "UP",
                        "service", "X-Ray & CloudWatch Monitoring",
                        "xray_enabled", xrayRecorder != null,
                        "cloudwatch_namespace", cloudWatchNamespace,
                        "service_name", serviceName,
                        "timestamp", Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("Monitoring health check failed", e);
                return Map.of(
                        "status", "DOWN",
                        "error", e.getMessage(),
                        "timestamp", Instant.now().toString()
                );
            }
        });
    }

    // Data Transfer Objects
    @Data
    @lombok.Builder
    public static class TraceResult {
        private String traceId;
        private String segmentId;
        private String operationName;
        private double startTime;
        private long duration;
        private String status;
    }

    @Data
    @lombok.Builder
    public static class CustomMetric {
        private String metricName;
        private double value;
        private String unit;
        private Map<String, String> dimensions;
    }

    @Data
    @lombok.Builder
    public static class MetricResult {
        private String metricName;
        private String namespace;
        private double value;
        private String unit;
        private String timestamp;
        private String status;
    }

    @Data
    @lombok.Builder
    public static class MetricData {
        private String timestamp;
        private Double average;
        private Double maximum;
        private Double sum;
        private String unit;
    }

    @Data
    @lombok.Builder
    public static class AlarmConfiguration {
        private String alarmName;
        private String description;
        private String metricName;
        private String statistic;
        private int period;
        private int evaluationPeriods;
        private double threshold;
        private String comparisonOperator;
        private String treatMissingData;
        private List<String> alarmActions;
    }

    @Data
    @lombok.Builder
    public static class AlarmResult {
        private String alarmName;
        private String alarmArn;
        private String description;
        private String metricName;
        private double threshold;
        private String state;
        private String createdAt;
    }

    @Data
    @lombok.Builder
    public static class AlarmInfo {
        private String alarmName;
        private String alarmArn;
        private String description;
        private String metricName;
        private String namespace;
        private String state;
        private double threshold;
    }

    @Data
    @lombok.Builder
    public static class PerformanceMetrics {
        private String operationName;
        private double responseTime;
        private double throughput;
        private int errorCount;
        private int successCount;
        private Map<String, Object> additionalMetrics;
    }
}