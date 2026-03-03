package com.admin.service.azure;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure Blob Storage Service
 * Comprehensive blob storage operations including upload, download, metadata management,
 * shared access signatures, and container management
 */
@Profile({"azure", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureBlobStorageService {

    private final BlobServiceClient blobServiceClient;

    @Value("${azure.storage.container-name:college-admin-container}")
    private String defaultContainerName;

    @Value("${azure.storage.cdn-endpoint:}")
    private String cdnEndpoint;

    /**
     * Upload file to Azure Blob Storage with metadata
     */
    public CompletableFuture<Map<String, Object>> uploadFileAsync(MultipartFile file, String containerName, String blobName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Uploading file to Azure Blob Storage: container={}, blob={}", containerName, blobName);

                BlobContainerClient containerClient = getOrCreateContainerClient(containerName);
                BlobClient blobClient = containerClient.getBlobClient(blobName);

                // Upload with metadata
                Map<String, String> metadata = new HashMap<>();
                metadata.put("originalFileName", file.getOriginalFilename());
                metadata.put("contentType", file.getContentType());
                metadata.put("uploadTimestamp", LocalDateTime.now().toString());
                metadata.put("fileSize", String.valueOf(file.getSize()));
                metadata.put("uploadedBy", "college-admin-service");

                blobClient.upload(new ByteArrayInputStream(file.getBytes()), file.getSize(), true);
                blobClient.setMetadata(metadata);

                // Generate URLs
                String blobUrl = blobClient.getBlobUrl();
                String cdnUrl = cdnEndpoint.isEmpty() ? blobUrl : 
                    cdnEndpoint + "/" + containerName + "/" + blobName;

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("blobName", blobName);
                result.put("containerName", containerName);
                result.put("blobUrl", blobUrl);
                result.put("cdnUrl", cdnUrl);
                result.put("fileSize", file.getSize());
                result.put("contentType", file.getContentType());
                result.put("metadata", metadata);
                result.put("uploadTime", LocalDateTime.now());

                log.info("Successfully uploaded file to Azure Blob Storage: {}", blobName);
                return result;

            } catch (Exception e) {
                log.error("Failed to upload file to Azure Blob Storage: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Download file from Azure Blob Storage
     */
    public CompletableFuture<InputStream> downloadFileAsync(String containerName, String blobName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Downloading file from Azure Blob Storage: container={}, blob={}", containerName, blobName);

                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
                BlobClient blobClient = containerClient.getBlobClient(blobName);

                return blobClient.openInputStream();

            } catch (Exception e) {
                log.error("Failed to download file from Azure Blob Storage: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Generate Shared Access Signature (SAS) for temporary access
     */
    public CompletableFuture<Map<String, Object>> generateSasTokenAsync(String containerName, String blobName, int durationHours) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating SAS token for blob: container={}, blob={}, duration={}h", containerName, blobName, durationHours);

                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
                BlobClient blobClient = containerClient.getBlobClient(blobName);

                // Set SAS permissions and expiry
                BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);
                OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(durationHours);

                BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, sasPermission);
                String sasToken = blobClient.generateSas(sasValues);
                String sasUrl = blobClient.getBlobUrl() + "?" + sasToken;

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("sasToken", sasToken);
                result.put("sasUrl", sasUrl);
                result.put("expiryTime", expiryTime);
                result.put("durationHours", durationHours);
                result.put("generatedAt", LocalDateTime.now());

                log.info("Successfully generated SAS token for blob: {}", blobName);
                return result;

            } catch (Exception e) {
                log.error("Failed to generate SAS token: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * List all blobs in a container with metadata
     */
    public CompletableFuture<Map<String, Object>> listBlobsAsync(String containerName, String prefix) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Listing blobs in container: {}, prefix: {}", containerName, prefix);

                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

                ListBlobsOptions options = new ListBlobsOptions();
                if (prefix != null && !prefix.isEmpty()) {
                    options.setPrefix(prefix);
                }

                List<Map<String, Object>> blobs = containerClient.listBlobs(options, null)
                        .stream()
                        .map(this::convertBlobItemToMap)
                        .collect(Collectors.toList());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("containerName", containerName);
                result.put("prefix", prefix);
                result.put("blobCount", blobs.size());
                result.put("blobs", blobs);
                result.put("listTime", LocalDateTime.now());

                log.info("Successfully listed {} blobs in container: {}", blobs.size(), containerName);
                return result;

            } catch (Exception e) {
                log.error("Failed to list blobs in container: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Delete blob from Azure Blob Storage
     */
    public CompletableFuture<Map<String, Object>> deleteBlobAsync(String containerName, String blobName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Deleting blob from Azure Blob Storage: container={}, blob={}", containerName, blobName);

                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
                BlobClient blobClient = containerClient.getBlobClient(blobName);

                boolean deleted = blobClient.deleteIfExists();

                Map<String, Object> result = new HashMap<>();
                result.put("success", deleted);
                result.put("blobName", blobName);
                result.put("containerName", containerName);
                result.put("deleted", deleted);
                result.put("deleteTime", LocalDateTime.now());

                if (deleted) {
                    log.info("Successfully deleted blob: {}", blobName);
                } else {
                    log.warn("Blob not found for deletion: {}", blobName);
                }

                return result;

            } catch (Exception e) {
                log.error("Failed to delete blob: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Copy blob between containers or within container
     */
    public CompletableFuture<Map<String, Object>> copyBlobAsync(String sourceContainer, String sourceBlobName, 
                                                              String targetContainer, String targetBlobName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Copying blob: {}:{} -> {}:{}", sourceContainer, sourceBlobName, targetContainer, targetBlobName);

                BlobContainerClient sourceContainerClient = blobServiceClient.getBlobContainerClient(sourceContainer);
                BlobClient sourceBlobClient = sourceContainerClient.getBlobClient(sourceBlobName);

                BlobContainerClient targetContainerClient = getOrCreateContainerClient(targetContainer);
                BlobClient targetBlobClient = targetContainerClient.getBlobClient(targetBlobName);

                // Start copy operation (returns ETag of destination blob)
                String copyId = targetBlobClient.copyFromUrl(sourceBlobClient.getBlobUrl());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("copyId", copyId);
                result.put("sourceContainer", sourceContainer);
                result.put("sourceBlobName", sourceBlobName);
                result.put("targetContainer", targetContainer);
                result.put("targetBlobName", targetBlobName);
                result.put("copyTime", LocalDateTime.now());

                log.info("Successfully initiated blob copy with ID: {}", copyId);
                return result;

            } catch (Exception e) {
                log.error("Failed to copy blob: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Get blob properties and metadata
     */
    public CompletableFuture<Map<String, Object>> getBlobPropertiesAsync(String containerName, String blobName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Getting blob properties: container={}, blob={}", containerName, blobName);

                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
                BlobClient blobClient = containerClient.getBlobClient(blobName);

                BlobProperties properties = blobClient.getProperties();

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("blobName", blobName);
                result.put("containerName", containerName);
                result.put("contentType", properties.getContentType());
                result.put("contentLength", properties.getBlobSize());
                result.put("lastModified", properties.getLastModified());
                result.put("createdOn", properties.getCreationTime());
                result.put("etag", properties.getETag());
                result.put("metadata", properties.getMetadata());
                result.put("blobType", properties.getBlobType().toString());
                result.put("accessTier", properties.getAccessTier() != null ? properties.getAccessTier().toString() : null);
                result.put("queryTime", LocalDateTime.now());

                log.info("Successfully retrieved blob properties for: {}", blobName);
                return result;

            } catch (Exception e) {
                log.error("Failed to get blob properties: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Health check for Azure Blob Storage service
     */
    public CompletableFuture<Map<String, Object>> checkHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> health = new HashMap<>();
            try {
                // Try to get service properties to test connectivity
                blobServiceClient.getProperties();

                health.put("service", "Azure Blob Storage");
                health.put("status", "UP");
                health.put("timestamp", LocalDateTime.now());
                health.put("defaultContainer", defaultContainerName);
                health.put("cdnEnabled", !cdnEndpoint.isEmpty());

                log.debug("Azure Blob Storage health check passed");

            } catch (Exception e) {
                health.put("service", "Azure Blob Storage");
                health.put("status", "DOWN");
                health.put("error", e.getMessage());
                health.put("timestamp", LocalDateTime.now());

                log.error("Azure Blob Storage health check failed: {}", e.getMessage());
            }
            return health;
        });
    }

    // Helper methods

    private BlobContainerClient getOrCreateContainerClient(String containerName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient = blobServiceClient.createBlobContainer(containerName);
            log.info("Created new blob container: {}", containerName);
        }
        return containerClient;
    }

    private Map<String, Object> convertBlobItemToMap(BlobItem blobItem) {
        Map<String, Object> blobInfo = new HashMap<>();
        blobInfo.put("name", blobItem.getName());
        blobInfo.put("contentType", blobItem.getProperties().getContentType());
        blobInfo.put("contentLength", blobItem.getProperties().getContentLength());
        blobInfo.put("lastModified", blobItem.getProperties().getLastModified());
        blobInfo.put("createdOn", blobItem.getProperties().getCreationTime());
        blobInfo.put("etag", blobItem.getProperties().getETag());
        blobInfo.put("metadata", blobItem.getMetadata());
        blobInfo.put("blobType", blobItem.getProperties().getBlobType().toString());
        if (blobItem.getProperties().getAccessTier() != null) {
            blobInfo.put("accessTier", blobItem.getProperties().getAccessTier().toString());
        }
        return blobInfo;
    }

    private String generateUniqueFileName(String originalFileName, String directory) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String uniqueId = UUID.randomUUID().toString();
        String sanitizedDirectory = directory != null ? directory.replaceAll("[^a-zA-Z0-9/]", "") : "default";
        
        return String.format("%s/%s_%s%s", sanitizedDirectory, "college_admin", uniqueId, extension);
    }
}