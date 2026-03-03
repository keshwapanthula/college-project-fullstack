package com.admin.config;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
 * Azure Services Health Indicator
 * Integrates all 10 Azure services health checks with Spring Boot Actuator
 * Provides comprehensive health monitoring for Azure cloud services
 */
@Profile({"azure", "default"})
@Component("azureServices")
@RequiredArgsConstructor
@Slf4j
public class AzureServicesHealthIndicator implements HealthIndicator {

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

    @Override
    public Health health() {
        try {
            log.debug("Starting Azure services health check");
            
            // Execute all health checks in parallel with timeout
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

            // Wait for all health checks to complete with 30-second timeout
            CompletableFuture<Void> allHealthChecks = CompletableFuture.allOf(
                blobHealth, cognitiveHealth, apimHealth, eventGridHealth, searchHealth,
                appInsightsHealth, functionsHealth, cosmosHealth, logicAppsHealth, adHealth
            );

            try {
                allHealthChecks.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Some Azure health checks timed out or failed", e);
                return Health.down()
                    .withDetail("error", "Health check timeout or failure")
                    .withDetail("exception", e.getMessage())
                    .withDetail("platform", "Azure")
                    .withDetail("timestamp", java.time.LocalDateTime.now())
                    .build();
            }

            // Collect all health results
            Map<String, Object> blobResult = getHealthResult(blobHealth);
            Map<String, Object> cognitiveResult = getHealthResult(cognitiveHealth);
            Map<String, Object> apimResult = getHealthResult(apimHealth);
            Map<String, Object> eventGridResult = getHealthResult(eventGridHealth);
            Map<String, Object> searchResult = getHealthResult(searchHealth);
            Map<String, Object> appInsightsResult = getHealthResult(appInsightsHealth);
            Map<String, Object> functionsResult = getHealthResult(functionsHealth);
            Map<String, Object> cosmosResult = getHealthResult(cosmosHealth);
            Map<String, Object> logicAppsResult = getHealthResult(logicAppsHealth);
            Map<String, Object> adResult = getHealthResult(adHealth);

            // Determine overall health status
            boolean allHealthy = isServiceHealthy(blobResult) &&
                               isServiceHealthy(cognitiveResult) &&
                               isServiceHealthy(apimResult) &&
                               isServiceHealthy(eventGridResult) &&
                               isServiceHealthy(searchResult) &&
                               isServiceHealthy(appInsightsResult) &&
                               isServiceHealthy(functionsResult) &&
                               isServiceHealthy(cosmosResult) &&
                               isServiceHealthy(logicAppsResult) &&
                               isServiceHealthy(adResult);

            Health.Builder healthBuilder = allHealthy ? Health.up() : Health.down();
            
            return healthBuilder
                .withDetail("platform", "Azure")
                .withDetail("timestamp", java.time.LocalDateTime.now())
                .withDetail("totalServices", 10)
                .withDetail("healthyServices", countHealthyServices(
                    blobResult, cognitiveResult, apimResult, eventGridResult, searchResult,
                    appInsightsResult, functionsResult, cosmosResult, logicAppsResult, adResult))
                .withDetail("services", Map.of(
                    "blobStorage", blobResult,
                    "cognitiveServices", cognitiveResult,
                    "apiManagement", apimResult,
                    "eventGrid", eventGridResult,
                    "search", searchResult,
                    "applicationInsights", appInsightsResult,
                    "functions", functionsResult,
                    "cosmosDb", cosmosResult,
                    "logicApps", logicAppsResult,
                    "activeDirectory", adResult
                ))
                .build();

        } catch (Exception e) {
            log.error("Failed to perform Azure services health check", e);
            return Health.down()
                .withDetail("error", "Health check execution failed")
                .withDetail("exception", e.getMessage())
                .withDetail("platform", "Azure")
                .withDetail("timestamp", java.time.LocalDateTime.now())
                .build();
        }
    }

    private Map<String, Object> getHealthResult(CompletableFuture<Map<String, Object>> healthFuture) {
        try {
            return healthFuture.join();
        } catch (Exception e) {
            log.warn("Failed to get health result", e);
            return Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now()
            );
        }
    }

    private boolean isServiceHealthy(Map<String, Object> healthResult) {
        Object status = healthResult.get("status");
        return "UP".equals(status) || "HEALTHY".equals(status) || Boolean.TRUE.equals(status);
    }

    private int countHealthyServices(Map<String, Object>... healthResults) {
        int count = 0;
        for (Map<String, Object> result : healthResults) {
            if (isServiceHealthy(result)) {
                count++;
            }
        }
        return count;
    }
}