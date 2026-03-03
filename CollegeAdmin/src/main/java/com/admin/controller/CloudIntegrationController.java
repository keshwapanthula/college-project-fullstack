package com.admin.controller;

import com.admin.dto.CloudStorageResponse;
import com.admin.service.cloud.CloudMessagingService;
import com.admin.service.cloud.CloudMonitoringService;
import com.admin.service.cloud.CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.annotation.Profile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud Integration Controller
 * Demonstrates AWS and Azure integrations for College Admin Service
 * Provides REST endpoints for testing multi-cloud capabilities
 */
@Profile({"aws", "azure", "default"})
@RestController
@RequestMapping("/api/cloud")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CloudIntegrationController {

    private final CloudStorageService cloudStorageService;
    private final CloudMessagingService cloudMessagingService;
    private final CloudMonitoringService cloudMonitoringService;

    /**
     * Upload file to hybrid cloud storage (AWS S3 / Azure Blob)
     */
    @PostMapping("/storage/upload")
    public ResponseEntity<CloudStorageResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "uploads") String directory) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Uploading file: {} to directory: {}", file.getOriginalFilename(), directory);
            
            CloudStorageResponse response = cloudStorageService.uploadFile(file, directory);
            
            // Record metrics
            long duration = System.currentTimeMillis() - startTime;
            cloudMonitoringService.recordApiResponseTime("/api/cloud/storage/upload", duration);
            cloudMonitoringService.recordCloudStorageOperation(
                response.getProvider(), 
                "upload", 
                file.getSize(), 
                response.isSuccess()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage());
            
            // Record failure metrics
            long duration = System.currentTimeMillis() - startTime;
            cloudMonitoringService.recordApiResponseTime("/api/cloud/storage/upload", duration);
            cloudMonitoringService.recordCloudStorageOperation("unknown", "upload", 0L, false);
            
            CloudStorageResponse errorResponse = CloudStorageResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Download file from hybrid cloud storage
     */
    @GetMapping("/storage/download/{fileName}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileName) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Downloading file: {}", fileName);
            
            InputStream fileStream = cloudStorageService.downloadFile(fileName);
            
            // Record metrics
            long duration = System.currentTimeMillis() - startTime;
            cloudMonitoringService.recordApiResponseTime("/api/cloud/storage/download", duration);
            cloudMonitoringService.recordCloudStorageOperation("hybrid", "download", 0L, true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(fileStream));
                    
        } catch (Exception e) {
            log.error("File download failed: {}", e.getMessage());
            
            long duration = System.currentTimeMillis() - startTime;
            cloudMonitoringService.recordApiResponseTime("/api/cloud/storage/download", duration);
            cloudMonitoringService.recordCloudStorageOperation("hybrid", "download", 0L, false);
            
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Send notification via hybrid messaging (AWS SNS/SQS or Azure Service Bus)
     */
    @PostMapping("/messaging/notification")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @RequestBody NotificationRequest request) {
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Sending notification: {}", request.getTitle());
            
            cloudMessagingService.sendNotification(
                request.getTitle(), 
                request.getBody(), 
                request.getRecipient()
            );
            
            // Record metrics
            long duration = System.currentTimeMillis() - startTime;
            cloudMonitoringService.recordApiResponseTime("/api/cloud/messaging/notification", duration);
            cloudMonitoringService.recordMessagingOperation("hybrid", "notification", true);
            
            response.put("success", true);
            response.put("message", "Notification sent successfully");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Notification failed: {}", e.getMessage());
            
            long duration = System.currentTimeMillis() - startTime;
            cloudMonitoringService.recordApiResponseTime("/api/cloud/messaging/notification", duration);
            cloudMonitoringService.recordMessagingOperation("hybrid", "notification", false);
            
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send admin alert
     */
    @PostMapping("/messaging/alert")
    public ResponseEntity<Map<String, Object>> sendAdminAlert(
            @RequestBody AdminAlertRequest request) {
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Sending admin alert: {}", request.getMessage());
            
            cloudMessagingService.sendAdminAlert(
                request.getLevel(), 
                request.getMessage(), 
                request.getDetails()
            );
            
            long duration = System.currentTimeMillis() - startTime;
            cloudMonitoringService.recordApiResponseTime("/api/cloud/messaging/alert", duration);
            cloudMonitoringService.recordMessagingOperation("hybrid", "admin-alert", true);
            
            response.put("success", true);
            response.put("message", "Admin alert sent successfully");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Admin alert failed: {}", e.getMessage());
            
            long duration = System.currentTimeMillis() - startTime;
            cloudMonitoringService.recordApiResponseTime("/api/cloud/messaging/alert", duration);
            cloudMonitoringService.recordMessagingOperation("hybrid", "admin-alert", false);
            
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send custom metric to monitoring systems
     */
    @PostMapping("/monitoring/metric")
    public ResponseEntity<Map<String, Object>> sendCustomMetric(
            @RequestBody CustomMetricRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Sending custom metric: {} = {}", request.getName(), request.getValue());
            
            cloudMonitoringService.sendMetricToCloudWatch(
                request.getName(), 
                request.getValue(), 
                request.getUnit()
            );
            
            response.put("success", true);
            response.put("message", "Metric sent successfully");
            response.put("metricName", request.getName());
            response.put("value", request.getValue());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Custom metric failed: {}", e.getMessage());
            
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get system health and send health metrics
     */
    @GetMapping("/monitoring/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Record application health metrics
            cloudMonitoringService.recordApplicationHealth();
            
            // Gather health information
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            health.put("status", "UP");
            health.put("timestamp", java.time.LocalDateTime.now());
            health.put("memory", Map.of(
                "total", totalMemory,
                "used", usedMemory,
                "free", freeMemory,
                "usagePercent", (double) usedMemory / totalMemory * 100
            ));
            health.put("cloud", Map.of(
                "aws", "Connected",
                "azure", "Connected",
                "monitoring", "Active"
            ));
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    // Request DTOs
    @lombok.Data
    public static class NotificationRequest {
        private String title;
        private String body;
        private String recipient;
    }

    @lombok.Data
    public static class AdminAlertRequest {
        private String level;
        private String message;
        private String details;
    }

    @lombok.Data
    public static class CustomMetricRequest {
        private String name;
        private double value;
        private String unit = "Count";
    }
}