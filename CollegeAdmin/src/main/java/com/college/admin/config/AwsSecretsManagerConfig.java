package com.college.admin.config;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

/**
 * AWS Secrets Manager Configuration
 * 
 * This configuration class loads database credentials and other sensitive
 * configuration from AWS Secrets Manager and makes them available to the
 * Spring Boot application.
 * 
 * Prerequisites:
 * - EC2 instance must have IAM role with SecretsManagerReadWrite policy
 * - Secret must exist in AWS Secrets Manager with name specified in properties
 * 
 * @author Keshwa Panthula
 * @since 2025-02-26
 */
@Slf4j
@Getter
@Configuration
@Profile("aws")
@ConditionalOnProperty(name = "aws.secretsmanager.enabled", havingValue = "true", matchIfMissing = false)
public class AwsSecretsManagerConfig {

    @Value("${aws.secretsmanager.secret-name}")
    private String secretName;

    @Value("${aws.region}")
    private String region;

    private SecretsManagerClient secretsClient;
    private JsonNode secretValues;

    /**
     * Initialize AWS Secrets Manager client and load secrets
     * Sets system properties for Spring Boot to use in application.properties
     */
    @PostConstruct
    public void init() {
        log.info("🔐 Initializing AWS Secrets Manager configuration...");
        log.info("   📍 Region: {}", region);
        log.info("   🔑 Secret Name: {}", secretName);

        try {
            // Create Secrets Manager client with default credentials provider
            // This will use EC2 instance profile credentials
            secretsClient = SecretsManagerClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            // Retrieve secret value
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            log.info("📥 Fetching secret from AWS Secrets Manager...");
            GetSecretValueResponse getSecretValueResponse = secretsClient.getSecretValue(getSecretValueRequest);
            String secret = getSecretValueResponse.secretString();

            // Parse JSON secret
            ObjectMapper objectMapper = new ObjectMapper();
            secretValues = objectMapper.readTree(secret);

            log.info("✅ Successfully loaded secrets from AWS Secrets Manager");

            // Set system properties for Spring Boot datasource configuration
            if (secretValues.has("host")) {
                System.setProperty("AWS_RDS_ENDPOINT", secretValues.get("host").asText());
                log.info("   🌐 RDS Endpoint: {}", secretValues.get("host").asText());
            }
            
            if (secretValues.has("username")) {
                System.setProperty("AWS_RDS_USERNAME", secretValues.get("username").asText());
                log.info("   👤 Username: {}", secretValues.get("username").asText());
            }
            
            if (secretValues.has("password")) {
                System.setProperty("AWS_RDS_PASSWORD", secretValues.get("password").asText());
                log.info("   🔒 Password: ******** (loaded successfully)");
            }

            if (secretValues.has("port")) {
                log.info("   🔌 Port: {}", secretValues.get("port").asText());
            }

            if (secretValues.has("dbname")) {
                log.info("   🗄️  Database: {}", secretValues.get("dbname").asText());
            }

            log.info("🎉 AWS Secrets Manager initialization complete!");

        } catch (SecretsManagerException e) {
            log.error("❌ Failed to load secrets from AWS Secrets Manager", e);
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("   Error Message: {}", e.awsErrorDetails().errorMessage());
            log.error("   🔍 Troubleshooting:");
            log.error("      1. Check IAM role has SecretsManagerReadWrite policy");
            log.error("      2. Verify secret name: {}", secretName);
            log.error("      3. Ensure secret exists in region: {}", region);
            throw new RuntimeException("Failed to initialize AWS Secrets Manager", e);
        } catch (IOException e) {
            log.error("❌ Failed to parse secret JSON", e);
            throw new RuntimeException("Failed to parse AWS Secrets Manager secret", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error during AWS Secrets Manager initialization", e);
            throw new RuntimeException("Failed to initialize AWS Secrets Manager", e);
        }
    }

    /**
     * Create SecretsManagerClient bean for dependency injection
     */
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return secretsClient;
    }

    /**
     * Get a specific secret value by key
     * 
     * @param key The key name in the secret JSON
     * @return The secret value as String
     */
    public String getSecretValue(String key) {
        if (secretValues == null) {
            log.warn("⚠️  Secret values not loaded. Returning null for key: {}", key);
            return null;
        }
        
        if (!secretValues.has(key)) {
            log.warn("⚠️  Key '{}' not found in secrets", key);
            return null;
        }
        
        return secretValues.get(key).asText();
    }

    /**
     * Get all secret values as JsonNode
     * 
     * @return JsonNode containing all secrets
     */
    public JsonNode getAllSecrets() {
        return secretValues;
    }

    /**
     * Check if secrets are loaded
     * 
     * @return true if secrets are successfully loaded
     */
    public boolean isSecretsLoaded() {
        return secretValues != null;
    }

    /**
     * Clean up AWS Secrets Manager client on shutdown
     */
    @PreDestroy
    public void cleanup() {
        if (secretsClient != null) {
            try {
                secretsClient.close();
                log.info("🔐 AWS Secrets Manager client closed successfully");
            } catch (Exception e) {
                log.error("⚠️  Error closing Secrets Manager client", e);
            }
        }
    }
}
