package com.admin.service.cloud;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.ComparisonOperator;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

/**
 * Cloud Monitoring Service for AWS CloudWatch and Azure Monitor
 * Provides comprehensive monitoring capabilities for multi-cloud environments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudMonitoringService {

    private final CloudWatchClient cloudWatchClient;

    @Value("${aws.cloudwatch.namespace:CollegeAdmin}")
    private String cloudWatchNamespace;

    @Value("${spring.application.name:college-admin}")
    private String applicationName;

    /**
     * Send custom metric to AWS CloudWatch
     */
    public void sendMetricToCloudWatch(String metricName, double value, String unit) {
        try {
            MetricDatum metricDatum = MetricDatum.builder()
                    .metricName(metricName)
                    .value(value)
                    .unit(StandardUnit.fromValue(unit))
                    .timestamp(Instant.now())
                    .dimensions(
                            Dimension.builder()
                                    .name("Service")
                                    .value(applicationName)
                                    .build(),
                            Dimension.builder()
                                    .name("Environment")
                                    .value("production")
                                    .build()
                    )
                    .build();

            PutMetricDataRequest putMetricDataRequest = PutMetricDataRequest.builder()
                    .namespace(cloudWatchNamespace)
                    .metricData(metricDatum)
                    .build();

            cloudWatchClient.putMetricData(putMetricDataRequest);
            log.debug("Metric sent to CloudWatch: {} = {}", metricName, value);

        } catch (Exception e) {
            log.error("Failed to send metric to CloudWatch: {}", e.getMessage());
        }
    }

    /**
     * Send business metrics for college admin operations
     */
    public void recordStudentOperation(String operation, boolean success) {
        sendMetricToCloudWatch("StudentOperations", 1.0, "Count");
        sendMetricToCloudWatch("StudentOperations." + operation, 1.0, "Count");
        
        if (success) {
            sendMetricToCloudWatch("StudentOperations.Success", 1.0, "Count");
        } else {
            sendMetricToCloudWatch("StudentOperations.Failure", 1.0, "Count");
        }
    }

    /**
     * Record API response time
     */
    public void recordApiResponseTime(String endpoint, long responseTimeMs) {
        sendMetricToCloudWatch("ApiResponseTime", responseTimeMs, "Milliseconds");
        sendMetricToCloudWatch("ApiResponseTime." + sanitizeEndpointName(endpoint), responseTimeMs, "Milliseconds");
    }

    /**
     * Record database operation metrics
     */
    public void recordDatabaseOperation(String operation, long durationMs, boolean success) {
        sendMetricToCloudWatch("DatabaseOperations", 1.0, "Count");
        sendMetricToCloudWatch("DatabaseOperations." + operation, 1.0, "Count");
        sendMetricToCloudWatch("DatabaseDuration", durationMs, "Milliseconds");
        
        if (success) {
            sendMetricToCloudWatch("DatabaseOperations.Success", 1.0, "Count");
        } else {
            sendMetricToCloudWatch("DatabaseOperations.Failure", 1.0, "Count");
        }
    }

    /**
     * Record cloud storage operations
     */
    public void recordCloudStorageOperation(String provider, String operation, long fileSizeBytes, boolean success) {
        String providerPrefix = provider.toLowerCase().replace(" ", "");
        
        sendMetricToCloudWatch("CloudStorage.Operations", 1.0, "Count");
        sendMetricToCloudWatch("CloudStorage." + providerPrefix + ".Operations", 1.0, "Count");
        sendMetricToCloudWatch("CloudStorage." + providerPrefix + "." + operation, 1.0, "Count");
        sendMetricToCloudWatch("CloudStorage.FileSize", fileSizeBytes, "Bytes");
        
        if (success) {
            sendMetricToCloudWatch("CloudStorage.Success", 1.0, "Count");
        } else {
            sendMetricToCloudWatch("CloudStorage.Failure", 1.0, "Count");
        }
    }

    /**
     * Record messaging operations
     */
    public void recordMessagingOperation(String provider, String messageType, boolean success) {
        String providerPrefix = provider.toLowerCase().replace(" ", "");
        
        sendMetricToCloudWatch("Messaging.Operations", 1.0, "Count");
        sendMetricToCloudWatch("Messaging." + providerPrefix + ".Operations", 1.0, "Count");
        sendMetricToCloudWatch("Messaging." + messageType, 1.0, "Count");
        
        if (success) {
            sendMetricToCloudWatch("Messaging.Success", 1.0, "Count");
        } else {
            sendMetricToCloudWatch("Messaging.Failure", 1.0, "Count");
        }
    }

    /**
     * Create CloudWatch alarm for critical metrics
     */
    public void createCriticalMetricAlarm(String alarmName, String metricName, double threshold) {
        try {
            PutMetricAlarmRequest alarmRequest = PutMetricAlarmRequest.builder()
                    .alarmName(alarmName)
                    .alarmDescription("Critical metric alarm for " + metricName)
                    .metricName(metricName)
                    .namespace(cloudWatchNamespace)
                    .statistic(Statistic.AVERAGE)
                    .period(300) // 5 minutes
                    .evaluationPeriods(2)
                    .threshold(threshold)
                    .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                    .dimensions(
                            Dimension.builder()
                                    .name("Service")
                                    .value(applicationName)
                                    .build()
                    )
                    .build();

            cloudWatchClient.putMetricAlarm(alarmRequest);
            log.info("CloudWatch alarm created: {}", alarmName);

        } catch (Exception e) {
            log.error("Failed to create CloudWatch alarm: {}", e.getMessage());
        }
    }

    /**
     * Send application health metrics
     */
    public void recordApplicationHealth() {
        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        sendMetricToCloudWatch("JVM.Memory.Total", totalMemory, "Bytes");
        sendMetricToCloudWatch("JVM.Memory.Used", usedMemory, "Bytes");
        sendMetricToCloudWatch("JVM.Memory.Free", freeMemory, "Bytes");
        sendMetricToCloudWatch("JVM.Memory.UsagePercent", (double) usedMemory / totalMemory * 100, "Percent");
        
        // Application status
        sendMetricToCloudWatch("Application.Health", 1.0, "Count");
    }

    private String sanitizeEndpointName(String endpoint) {
        return endpoint.replaceAll("[^a-zA-Z0-9]", "_");
    }
}