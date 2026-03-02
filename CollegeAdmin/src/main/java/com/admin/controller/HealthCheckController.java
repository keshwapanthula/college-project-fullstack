package com.admin.controller;

import com.admin.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Multi-Cloud Health Check Controller
 * Provides comprehensive health monitoring endpoints for AWS, Azure, and GCP services
 * Enables real-time monitoring and status reporting across all cloud platforms
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    /**
     * Comprehensive multi-cloud health check
     * Returns health status across all AWS, Azure, and GCP services
     */
    @GetMapping("/all")
    public CompletableFuture<ResponseEntity<Object>> getAllCloudServicesHealth() {
        log.info("Multi-cloud health check requested");
        return healthCheckService.checkAllCloudServicesHealth()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    /**
     * GCP services health check
     */
    @GetMapping("/gcp")
    public CompletableFuture<ResponseEntity<Object>> getGcpServicesHealth() {
        log.debug("GCP services health check requested");
        return healthCheckService.checkGcpServicesHealth()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    /**
     * AWS services health check
     */
    @GetMapping("/aws")
    public CompletableFuture<ResponseEntity<Object>> getAwsServicesHealth() {
        log.debug("AWS services health check requested");
        return healthCheckService.checkAwsServicesHealth()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    /**
     * Azure services health check
     */
    @GetMapping("/azure")
    public CompletableFuture<ResponseEntity<Object>> getAzureServicesHealth() {
        log.debug("Azure services health check requested");
        return healthCheckService.checkAzureServicesHealth()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    /**
     * Service health summary
     */
    @GetMapping("/summary")
    public CompletableFuture<ResponseEntity<Object>> getServiceHealthSummary() {
        log.debug("Service health summary requested");
        return healthCheckService.getServiceHealthSummary()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    /**
     * Simple health ping endpoint
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> healthPing() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "College Admin Multi-Cloud Platform",
            "timestamp", java.time.LocalDateTime.now(),
            "version", "1.0.0",
            "platforms", Map.of(
                "gcp", "Google Cloud Platform",
                "aws", "Amazon Web Services", 
                "azure", "Microsoft Azure"
            )
        ));
    }
}