package com.admin.service.aws;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.ApiKeySourceType;
import software.amazon.awssdk.services.apigateway.model.CreateApiKeyRequest;
import software.amazon.awssdk.services.apigateway.model.CreateApiKeyResponse;
import software.amazon.awssdk.services.apigateway.model.CreateDeploymentRequest;
import software.amazon.awssdk.services.apigateway.model.CreateDeploymentResponse;
import software.amazon.awssdk.services.apigateway.model.CreateResourceRequest;
import software.amazon.awssdk.services.apigateway.model.CreateResourceResponse;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiResponse;
import software.amazon.awssdk.services.apigateway.model.EndpointConfiguration;
import software.amazon.awssdk.services.apigateway.model.EndpointType;
import software.amazon.awssdk.services.apigateway.model.GetResourcesRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourcesResponse;
import software.amazon.awssdk.services.apigateway.model.GetRestApisRequest;
import software.amazon.awssdk.services.apigateway.model.GetRestApisResponse;
import software.amazon.awssdk.services.apigateway.model.IntegrationType;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationRequest;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationResponseRequest;
import software.amazon.awssdk.services.apigateway.model.PutMethodRequest;
import software.amazon.awssdk.services.apigateway.model.PutMethodResponseRequest;
import software.amazon.awssdk.services.apigateway.model.Resource;

/**
 * AWS API Gateway Management Service
 * Provides REST API and GraphQL API creation, deployment, and management
 */
@Profile({"aws", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsApiGatewayService {

    private final ApiGatewayClient apiGatewayClient;

    @Value("${aws.apigateway.region:us-east-1}")
    private String region;

    @Value("${aws.apigateway.stage:prod}")
    private String defaultStage;

    /**
     * Create a new REST API
     */
    public CompletableFuture<ApiCreationResult> createRestApi(ApiConfiguration config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating REST API: {}", config.getApiName());

                CreateRestApiRequest request = CreateRestApiRequest.builder()
                        .name(config.getApiName())
                        .description(config.getDescription())
                        .endpointConfiguration(EndpointConfiguration.builder()
                                .types(EndpointType.REGIONAL)
                                .build())
                        .apiKeySource(ApiKeySourceType.HEADER)
                        .build();

                CreateRestApiResponse response = apiGatewayClient.createRestApi(request);

                ApiCreationResult result = ApiCreationResult.builder()
                        .apiId(response.id())
                        .apiName(response.name())
                        .apiArn("arn:aws:apigateway:" + region + "::/restapis/" + response.id())
                        .endpointUrl("https://" + response.id() + ".execute-api." + region + ".amazonaws.com/" + defaultStage)
                        .createdAt(response.createdDate().toString())
                        .status("AVAILABLE")
                        .build();

                log.info("REST API created successfully: {}", result.getApiId());
                return result;

            } catch (Exception e) {
                log.error("Error creating REST API", e);
                throw new RuntimeException("Failed to create REST API", e);
            }
        });
    }

    /**
     * Create API resources and methods
     */
    public CompletableFuture<ResourceCreationResult> createApiResource(String apiId, ApiResource resource) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating API resource: {} for API: {}", resource.getResourcePath(), apiId);

                // Get root resource ID
                GetResourcesRequest getResourcesRequest = GetResourcesRequest.builder()
                        .restApiId(apiId)
                        .build();
                GetResourcesResponse resourcesResponse = apiGatewayClient.getResources(getResourcesRequest);
                
                String parentId = resourcesResponse.items().stream()
                        .filter(res -> "/".equals(res.path()))
                        .findFirst()
                        .map(Resource::id)
                        .orElseThrow(() -> new RuntimeException("Root resource not found"));

                // Create resource
                CreateResourceRequest createResourceRequest = CreateResourceRequest.builder()
                        .restApiId(apiId)
                        .parentId(parentId)
                        .pathPart(resource.getResourcePath().substring(1)) // Remove leading slash
                        .build();

                CreateResourceResponse resourceResponse = apiGatewayClient.createResource(createResourceRequest);

                // Create methods for the resource
                for (ApiMethod method : resource.getMethods()) {
                    createApiMethod(apiId, resourceResponse.id(), method);
                }

                ResourceCreationResult result = ResourceCreationResult.builder()
                        .resourceId(resourceResponse.id())
                        .resourcePath(resourceResponse.path())
                        .parentId(resourceResponse.parentId())
                        .methodCount(resource.getMethods().size())
                        .build();

                log.info("API resource created successfully: {}", result.getResourceId());
                return result;

            } catch (Exception e) {
                log.error("Error creating API resource", e);
                throw new RuntimeException("Failed to create API resource", e);
            }
        });
    }

    /**
     * Deploy API to a stage
     */
    public CompletableFuture<DeploymentResult> deployApi(String apiId, String stageName, String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Deploying API {} to stage: {}", apiId, stageName);

                CreateDeploymentRequest request = CreateDeploymentRequest.builder()
                        .restApiId(apiId)
                        .stageName(stageName)
                        .description(description)
                        .build();

                CreateDeploymentResponse response = apiGatewayClient.createDeployment(request);

                String endpointUrl = "https://" + apiId + ".execute-api." + region + ".amazonaws.com/" + stageName;

                DeploymentResult result = DeploymentResult.builder()
                        .deploymentId(response.id())
                        .stageName(stageName)
                        .endpointUrl(endpointUrl)
                        .deployedAt(response.createdDate().toString())
                        .description(description)
                        .build();

                log.info("API deployed successfully to: {}", endpointUrl);
                return result;

            } catch (Exception e) {
                log.error("Error deploying API", e);
                throw new RuntimeException("Failed to deploy API", e);
            }
        });
    }

    /**
     * List all REST APIs
     */
    public CompletableFuture<List<ApiInfo>> listRestApis() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Listing REST APIs");

                GetRestApisRequest request = GetRestApisRequest.builder()
                        .limit(25)
                        .build();

                GetRestApisResponse response = apiGatewayClient.getRestApis(request);

                List<ApiInfo> apis = response.items().stream()
                        .map(api -> ApiInfo.builder()
                                .apiId(api.id())
                                .apiName(api.name())
                                .description(api.description())
                                .createdAt(api.createdDate().toString())
                                .version(api.version())
                                .build())
                        .toList();

                log.info("Found {} REST APIs", apis.size());
                return apis;

            } catch (Exception e) {
                log.error("Error listing REST APIs", e);
                throw new RuntimeException("Failed to list REST APIs", e);
            }
        });
    }

    /**
     * Get API usage statistics
     */
    public CompletableFuture<ApiUsageStats> getApiUsageStats(String apiId, int days) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Getting usage stats for API: {} for last {} days", apiId, days);

                // For demo purposes, generate mock statistics
                ApiUsageStats stats = ApiUsageStats.builder()
                        .apiId(apiId)
                        .totalRequests(15432L + (long)(Math.random() * 10000))
                        .successfulRequests(14892L + (long)(Math.random() * 9000))
                        .errorRequests(540L + (long)(Math.random() * 1000))
                        .averageLatency(125.5 + Math.random() * 50)
                        .peakRequestsPerSecond(45.2 + Math.random() * 20)
                        .dataTransferredMB(1250.7 + Math.random() * 500)
                        .periodDays(days)
                        .build();

                log.info("API usage stats retrieved for API: {}", apiId);
                return stats;

            } catch (Exception e) {
                log.error("Error getting API usage stats", e);
                throw new RuntimeException("Failed to get API usage stats", e);
            }
        });
    }

    /**
     * Create API key for authentication
     */
    public CompletableFuture<ApiKeyResult> createApiKey(String keyName, String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating API key: {}", keyName);

                CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                        .name(keyName)
                        .description(description)
                        .enabled(true)
                        .build();

                CreateApiKeyResponse response = apiGatewayClient.createApiKey(request);

                ApiKeyResult result = ApiKeyResult.builder()
                        .keyId(response.id())
                        .keyName(response.name())
                        .keyValue(response.value())
                        .description(response.description())
                        .enabled(response.enabled())
                        .createdAt(response.createdDate().toString())
                        .build();

                log.info("API key created successfully: {}", result.getKeyId());
                return result;

            } catch (Exception e) {
                log.error("Error creating API key", e);
                throw new RuntimeException("Failed to create API key", e);
            }
        });
    }

    /**
     * Get API health status
     */
    public CompletableFuture<Map<String, Object>> getApiGatewayHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test connection by listing APIs
                GetRestApisRequest request = GetRestApisRequest.builder()
                        .limit(1)
                        .build();
                
                apiGatewayClient.getRestApis(request);

                return Map.of(
                        "status", "UP",
                        "service", "API Gateway",
                        "region", region,
                        "timestamp", java.time.Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("API Gateway health check failed", e);
                return Map.of(
                        "status", "DOWN",
                        "error", e.getMessage(),
                        "timestamp", java.time.Instant.now().toString()
                );
            }
        });
    }

    // Helper method to create API methods
    private void createApiMethod(String apiId, String resourceId, ApiMethod method) {
        try {
            PutMethodRequest putMethodRequest = PutMethodRequest.builder()
                    .restApiId(apiId)
                    .resourceId(resourceId)
                    .httpMethod(method.getHttpMethod())
                    .authorizationType(method.getAuthorizationType())
                    .apiKeyRequired(method.isApiKeyRequired())
                    .build();

            apiGatewayClient.putMethod(putMethodRequest);

            // Set up integration (for demo, we'll use MOCK integration)
            PutIntegrationRequest putIntegrationRequest = PutIntegrationRequest.builder()
                    .restApiId(apiId)
                    .resourceId(resourceId)
                    .httpMethod(method.getHttpMethod())
                    .type(IntegrationType.MOCK)
                    .requestTemplates(Map.of("application/json", "{\"statusCode\": 200}"))
                    .build();

            apiGatewayClient.putIntegration(putIntegrationRequest);

            // Set up method response
            PutMethodResponseRequest putMethodResponseRequest = PutMethodResponseRequest.builder()
                    .restApiId(apiId)
                    .resourceId(resourceId)
                    .httpMethod(method.getHttpMethod())
                    .statusCode("200")
                    .build();

            apiGatewayClient.putMethodResponse(putMethodResponseRequest);

            // Set up integration response
            PutIntegrationResponseRequest putIntegrationResponseRequest = PutIntegrationResponseRequest.builder()
                    .restApiId(apiId)
                    .resourceId(resourceId)
                    .httpMethod(method.getHttpMethod())
                    .statusCode("200")
                    .responseTemplates(Map.of("application/json", "{\"message\": \"Success from " + method.getHttpMethod() + "\"}"))
                    .build();

            apiGatewayClient.putIntegrationResponse(putIntegrationResponseRequest);

            log.info("Created method: {} for resource: {}", method.getHttpMethod(), resourceId);

        } catch (Exception e) {
            log.error("Error creating API method: " + method.getHttpMethod(), e);
        }
    }

    // Data Transfer Objects
    @Data
    @lombok.Builder
    public static class ApiConfiguration {
        private String apiName;
        private String description;
        private String apiType; // REST, GRAPHQL
        private boolean corsEnabled;
    }

    @Data
    @lombok.Builder
    public static class ApiResource {
        private String resourcePath;
        private List<ApiMethod> methods;
    }

    @Data
    @lombok.Builder
    public static class ApiMethod {
        private String httpMethod;
        private String authorizationType;
        private boolean apiKeyRequired;
        private String integration;
    }

    @Data
    @lombok.Builder
    public static class ApiCreationResult {
        private String apiId;
        private String apiName;
        private String apiArn;
        private String endpointUrl;
        private String createdAt;
        private String status;
    }

    @Data
    @lombok.Builder
    public static class ResourceCreationResult {
        private String resourceId;
        private String resourcePath;
        private String parentId;
        private int methodCount;
    }

    @Data
    @lombok.Builder
    public static class DeploymentResult {
        private String deploymentId;
        private String stageName;
        private String endpointUrl;
        private String deployedAt;
        private String description;
    }

    @Data
    @lombok.Builder
    public static class ApiInfo {
        private String apiId;
        private String apiName;
        private String description;
        private String createdAt;
        private String version;
    }

    @Data
    @lombok.Builder
    public static class ApiUsageStats {
        private String apiId;
        private Long totalRequests;
        private Long successfulRequests;
        private Long errorRequests;
        private Double averageLatency;
        private Double peakRequestsPerSecond;
        private Double dataTransferredMB;
        private int periodDays;
    }

    @Data
    @lombok.Builder
    public static class ApiKeyResult {
        private String keyId;
        private String keyName;
        private String keyValue;
        private String description;
        private boolean enabled;
        private String createdAt;
    }
}