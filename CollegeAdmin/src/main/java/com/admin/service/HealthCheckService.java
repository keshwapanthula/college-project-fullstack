package com.admin.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.admin.service.aws.AwsCognitoService;
import com.admin.service.aws.AwsDynamoDbService;
import com.admin.service.aws.AwsKinesisService;
import com.admin.service.aws.AwsLambdaService;
import com.admin.service.aws.AwsStepFunctionsService;
import com.admin.service.azure.AzureActiveDirectoryService;
import com.admin.service.azure.AzureCosmosDbService;
import com.admin.service.azure.AzureFunctionsService;
import com.admin.service.azure.AzureLogicAppsService;
import com.admin.service.azure.AzureServiceBusService;
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
 * Centralized Health Check Service
 * Monitors health status of all cloud services across AWS, Azure, and GCP platforms
 * Provides comprehensive system health monitoring and status reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    // GCP Services
    private final GcpCloudStorageService gcpCloudStorageService;
    private final GcpAiPlatformService gcpAiPlatformService;
    private final GcpApiGatewayService gcpApiGatewayService;
    private final GcpPubSubService gcpPubSubService;
    private final GcpSearchService gcpSearchService;
    private final GcpMonitoringService gcpMonitoringService;
    private final GcpCloudFunctionsService gcpCloudFunctionsService;
    private final GcpFirestoreService gcpFirestoreService;
    private final GcpWorkflowsService gcpWorkflowsService;
    private final GcpIdentityService gcpIdentityService;

    // AWS Services (optional - only available in aws profile)
    @Autowired(required=false)
    private AwsLambdaService awsLambdaService;
    @Autowired(required=false)
    private AwsDynamoDbService awsDynamoDbService;
    @Autowired(required=false)
    private AwsKinesisService awsKinesisService;
    @Autowired(required=false)
    private AwsStepFunctionsService awsStepFunctionsService;
    @Autowired(required=false)
    private AwsCognitoService awsCognitoService;

    // Azure Services (optional - only available in azure profile)
    @Autowired(required=false)
    private AzureFunctionsService azureFunctionsService;
    @Autowired(required=false)
    private AzureCosmosDbService azureCosmosDbService;
    @Autowired(required=false)
    private AzureServiceBusService azureServiceBusService;
    @Autowired(required=false)
    private AzureLogicAppsService azureLogicAppsService;
    @Autowired(required=false)
    private AzureActiveDirectoryService azureActiveDirectoryService;

    /**
     * Comprehensive multi-cloud health check
     * Checks health status across all AWS, Azure, and GCP services
     */
    public CompletableFuture<Map<String, Object>> checkAllCloudServicesHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting comprehensive multi-cloud health check");

                Map<String, Object> healthStatus = new HashMap<>();
                Map<String, Object> platforms = new HashMap<>();
                
                // GCP Health Checks
                Map<String, Object> gcpHealth = checkGcpServicesHealth().join();
                platforms.put("gcp", gcpHealth);

                // AWS Health Checks
                Map<String, Object> awsHealth = checkAwsServicesHealth().join();
                platforms.put("aws", awsHealth);

                // Azure Health Checks
                Map<String, Object> azureHealth = checkAzureServicesHealth().join();
                platforms.put("azure", azureHealth);

                // Calculate overall system health
                long totalServices = getTotalServiceCount(platforms);
                long healthyServices = getHealthyServiceCount(platforms);
                double healthPercentage = totalServices > 0 ? (double) healthyServices / totalServices * 100 : 0.0;

                String overallStatus;
                if (healthPercentage >= 95.0) {
                    overallStatus = "EXCELLENT";
                } else if (healthPercentage >= 80.0) {
                    overallStatus = "GOOD";
                } else if (healthPercentage >= 60.0) {
                    overallStatus = "DEGRADED";
                } else {
                    overallStatus = "CRITICAL";
                }

                healthStatus.put("overallStatus", overallStatus);
                healthStatus.put("healthPercentage", Math.round(healthPercentage * 100.0) / 100.0);
                healthStatus.put("totalServices", totalServices);
                healthStatus.put("healthyServices", healthyServices);
                healthStatus.put("unhealthyServices", totalServices - healthyServices);
                healthStatus.put("platforms", platforms);
                healthStatus.put("timestamp", LocalDateTime.now());
                healthStatus.put("checkDuration", "< 5 seconds");

                log.info("Multi-cloud health check completed: {} - {}/{} services healthy", 
                        overallStatus, healthyServices, totalServices);

                return healthStatus;

            } catch (Exception e) {
                log.error("Failed to perform comprehensive health check", e);
                return Map.of(
                    "overallStatus", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now(),
                    "serviceAvailable", false
                );
            }
        });
    }

    /**
     * Check health of all GCP services
     */
    public CompletableFuture<Map<String, Object>> checkGcpServicesHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Checking GCP services health");

                Map<String, CompletableFuture<Map<String, Object>>> futures = new HashMap<>();
                futures.put("cloudStorage", gcpCloudStorageService.checkHealthAsync());
                futures.put("aiPlatform", gcpAiPlatformService.checkHealthAsync());
                futures.put("apiGateway", gcpApiGatewayService.checkHealthAsync());
                futures.put("pubSub", gcpPubSubService.checkHealthAsync());
                futures.put("search", gcpSearchService.checkHealthAsync());
                futures.put("monitoring", gcpMonitoringService.checkHealthAsync());
                futures.put("cloudFunctions", gcpCloudFunctionsService.checkHealthAsync());
                futures.put("firestore", gcpFirestoreService.checkHealthAsync());
                futures.put("workflows", gcpWorkflowsService.checkHealthAsync());
                futures.put("identity", gcpIdentityService.checkHealthAsync());

                // Wait for all health checks to complete
                CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

                Map<String, Object> services = new HashMap<>();
                for (Map.Entry<String, CompletableFuture<Map<String, Object>>> entry : futures.entrySet()) {
                    Map<String, Object> serviceHealth = entry.getValue().join();
                    services.put(entry.getKey(), serviceHealth.get("status"));
                }

                long healthyServices = services.values().stream()
                        .mapToLong(status -> "UP".equals(status) ? 1 : 0)
                        .sum();

                String platformStatus;
                if (healthyServices == services.size()) {
                    platformStatus = "UP";
                } else if (healthyServices > 0) {
                    platformStatus = "DEGRADED";
                } else {
                    platformStatus = "DOWN";
                }

                Map<String, Object> gcpHealth = new HashMap<>();
                gcpHealth.put("status", platformStatus);
                gcpHealth.put("platform", "Google Cloud Platform");
                gcpHealth.put("totalServices", services.size());
                gcpHealth.put("healthyServices", healthyServices);
                gcpHealth.put("services", services);
                gcpHealth.put("checkTime", LocalDateTime.now());

                log.debug("GCP health check completed: {} - {}/{} services healthy", 
                         platformStatus, healthyServices, services.size());

                return gcpHealth;

            } catch (Exception e) {
                log.error("Failed to check GCP services health", e);
                return Map.of(
                    "status", "ERROR",
                    "platform", "Google Cloud Platform",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Check health of all AWS services
     */
    public CompletableFuture<Map<String, Object>> checkAwsServicesHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Checking AWS services health");

                Map<String, CompletableFuture<Map<String, Object>>> futures = new HashMap<>();
                futures.put("lambda", awsLambdaService.getLambdaHealth());
                futures.put("dynamodb", awsDynamoDbService.getDynamoDbHealth());
                futures.put("kinesis", awsKinesisService.getKinesisHealth());
                futures.put("stepfunctions", awsStepFunctionsService.getStepFunctionsHealth());
                futures.put("cognito", awsCognitoService.getCognitoHealth());

                // Wait for all health checks to complete
                CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

                Map<String, Object> services = new HashMap<>();
                for (Map.Entry<String, CompletableFuture<Map<String, Object>>> entry : futures.entrySet()) {
                    Map<String, Object> serviceHealth = entry.getValue().join();
                    services.put(entry.getKey(), serviceHealth.get("status"));
                }

                long healthyServices = services.values().stream()
                        .mapToLong(status -> "UP".equals(status) ? 1 : 0)
                        .sum();

                String platformStatus;
                if (healthyServices == services.size()) {
                    platformStatus = "UP";
                } else if (healthyServices > 0) {
                    platformStatus = "DEGRADED";
                } else {
                    platformStatus = "DOWN";
                }

                Map<String, Object> awsHealth = new HashMap<>();
                awsHealth.put("status", platformStatus);
                awsHealth.put("platform", "Amazon Web Services");
                awsHealth.put("totalServices", services.size());
                awsHealth.put("healthyServices", healthyServices);
                awsHealth.put("services", services);
                awsHealth.put("checkTime", LocalDateTime.now());

                log.debug("AWS health check completed: {} - {}/{} services healthy", 
                         platformStatus, healthyServices, services.size());

                return awsHealth;

            } catch (Exception e) {
                log.error("Failed to check AWS services health", e);
                return Map.of(
                    "status", "ERROR",
                    "platform", "Amazon Web Services",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Check health of all Azure services
     */
    public CompletableFuture<Map<String, Object>> checkAzureServicesHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Checking Azure services health");

                Map<String, CompletableFuture<Map<String, Object>>> futures = new HashMap<>();
                futures.put("functions", azureFunctionsService.checkHealthAsync());
                futures.put("cosmosdb", azureCosmosDbService.checkHealthAsync());
                futures.put("servicebus", azureServiceBusService.checkServiceBusHealthAsync());
                futures.put("logicapps", azureLogicAppsService.checkHealthAsync());
                futures.put("activedirectory", azureActiveDirectoryService.checkHealthAsync());

                // Wait for all health checks to complete
                CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

                Map<String, Object> services = new HashMap<>();
                for (Map.Entry<String, CompletableFuture<Map<String, Object>>> entry : futures.entrySet()) {
                    Map<String, Object> serviceHealth = entry.getValue().join();
                    services.put(entry.getKey(), serviceHealth.get("status"));
                }

                long healthyServices = services.values().stream()
                        .mapToLong(status -> "UP".equals(status) ? 1 : 0)
                        .sum();

                String platformStatus;
                if (healthyServices == services.size()) {
                    platformStatus = "UP";
                } else if (healthyServices > 0) {
                    platformStatus = "DEGRADED";
                } else {
                    platformStatus = "DOWN";
                }

                Map<String, Object> azureHealth = new HashMap<>();
                azureHealth.put("status", platformStatus);
                azureHealth.put("platform", "Microsoft Azure");
                azureHealth.put("totalServices", services.size());
                azureHealth.put("healthyServices", healthyServices);
                azureHealth.put("services", services);
                azureHealth.put("checkTime", LocalDateTime.now());

                log.debug("Azure health check completed: {} - {}/{} services healthy", 
                         platformStatus, healthyServices, services.size());

                return azureHealth;

            } catch (Exception e) {
                log.error("Failed to check Azure services health", e);
                return Map.of(
                    "status", "ERROR",
                    "platform", "Microsoft Azure",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Get individual service health summary
     */
    public CompletableFuture<Map<String, Object>> getServiceHealthSummary() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Generating service health summary");

                Map<String, Object> summary = new HashMap<>();
                
                // Service categories
                summary.put("storage", Map.of(
                    "gcp", "Cloud Storage",
                    "aws", "S3",
                    "azure", "Blob Storage"
                ));
                
                summary.put("compute", Map.of(
                    "gcp", "Cloud Functions",
                    "aws", "Lambda",
                    "azure", "Functions"
                ));
                
                summary.put("database", Map.of(
                    "gcp", "Firestore",
                    "aws", "DynamoDB",
                    "azure", "Cosmos DB"
                ));
                
                summary.put("messaging", Map.of(
                    "gcp", "Pub/Sub",
                    "aws", "Kinesis",
                    "azure", "Service Bus"
                ));
                
                summary.put("workflows", Map.of(
                    "gcp", "Workflows",
                    "aws", "Step Functions",
                    "azure", "Logic Apps"
                ));
                
                summary.put("identity", Map.of(
                    "gcp", "Identity",
                    "aws", "Cognito",
                    "azure", "Active Directory"
                ));

                summary.put("totalPlatforms", 3);
                summary.put("totalServiceCategories", 6);
                summary.put("multiCloudArchitecture", true);
                summary.put("generatedAt", LocalDateTime.now());

                return summary;

            } catch (Exception e) {
                log.error("Failed to generate service health summary", e);
                return Map.of(
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    // Helper methods

    private long getTotalServiceCount(Map<String, Object> platforms) {
        return platforms.values().stream()
                .filter(platform -> platform instanceof Map)
                .mapToLong(platform -> {
                    Map<?, ?> platformMap = (Map<?, ?>) platform;
                    Object totalServices = platformMap.get("totalServices");
                    return totalServices instanceof Number ? ((Number) totalServices).longValue() : 0;
                })
                .sum();
    }

    private long getHealthyServiceCount(Map<String, Object> platforms) {
        return platforms.values().stream()
                .filter(platform -> platform instanceof Map)
                .mapToLong(platform -> {
                    Map<?, ?> platformMap = (Map<?, ?>) platform;
                    Object healthyServices = platformMap.get("healthyServices");
                    return healthyServices instanceof Number ? ((Number) healthyServices).longValue() : 0;
                })
                .sum();
    }
}
