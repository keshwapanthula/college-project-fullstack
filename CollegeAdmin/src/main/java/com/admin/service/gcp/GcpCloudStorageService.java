package com.admin.service.gcp;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BucketListOption;
import com.google.cloud.storage.StorageClass;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Cloud Storage Service
 * Handles file operations, bucket management, and object storage
 * Equivalent to AWS S3 and Azure Blob Storage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GcpCloudStorageService {

    private final Storage storage;

    /**
     * Upload file to Google Cloud Storage
     * Creates bucket if it doesn't exist
     */
    public CompletableFuture<Map<String, Object>> uploadFileAsync(MultipartFile file, String bucketName, String objectName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Uploading file to GCS: bucket={}, object={}, size={}", bucketName, objectName, file.getSize());

                // Create bucket if it doesn't exist
                createBucketIfNotExists(bucketName);

                // Set blob info with metadata
                BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                        .setContentType(file.getContentType())
                        .setMetadata(Map.of(
                            "originalName", file.getOriginalFilename(),
                            "uploadTime", LocalDateTime.now().toString(),
                            "uploadedBy", "college-admin-system",
                            "fileSize", String.valueOf(file.getSize())
                        ))
                        .build();

                // Upload the file
                Blob blob = storage.create(blobInfo, file.getBytes());

                log.info("File uploaded successfully to GCS: {}", blob.getName());

                return Map.of(
                    "success", true,
                    "bucketName", bucketName,
                    "objectName", objectName,
                    "blobId", blob.getBlobId().toString(),
                    "size", blob.getSize(),
                    "contentType", blob.getContentType(),
                    "gsUri", String.format("gs://%s/%s", bucketName, objectName),
                    "publicUrl", getPublicUrl(bucketName, objectName),
                    "uploadTime", LocalDateTime.now(),
                    "metadata", blob.getMetadata()
                );

            } catch (Exception e) {
                log.error("Failed to upload file to GCS", e);
                return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Download file from Google Cloud Storage
     */
    public CompletableFuture<Map<String, Object>> downloadFileAsync(String bucketName, String objectName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Downloading file from GCS: bucket={}, object={}", bucketName, objectName);

                BlobId blobId = BlobId.of(bucketName, objectName);
                Blob blob = storage.get(blobId);

                if (blob == null) {
                    return Map.of(
                        "success", false,
                        "error", "Object not found",
                        "bucketName", bucketName,
                        "objectName", objectName
                    );
                }

                byte[] content = blob.getContent();

                log.info("File downloaded successfully from GCS: {} bytes", content.length);

                return Map.of(
                    "success", true,
                    "bucketName", bucketName,
                    "objectName", objectName,
                    "content", content,
                    "contentType", blob.getContentType(),
                    "size", blob.getSize(),
                    "downloadTime", LocalDateTime.now(),
                    "metadata", blob.getMetadata()
                );

            } catch (Exception e) {
                log.error("Failed to download file from GCS", e);
                return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "bucketName", bucketName,
                    "objectName", objectName,
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Generate signed URL for temporary access
     */
    public CompletableFuture<Map<String, Object>> generateSignedUrlAsync(String bucketName, String objectName, long durationMinutes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating signed URL for GCS object: bucket={}, object={}, duration={}min", 
                        bucketName, objectName, durationMinutes);

                BlobId blobId = BlobId.of(bucketName, objectName);
                Blob blob = storage.get(blobId);

                if (blob == null) {
                    return Map.of(
                        "success", false,
                        "error", "Object not found",
                        "bucketName", bucketName,
                        "objectName", objectName
                    );
                }

                BlobInfo blobInfoForSign = BlobInfo.newBuilder(blobId).build();
                URL signedUrl = storage.signUrl(
                    blobInfoForSign,
                    durationMinutes,
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET)
                );

                log.info("Signed URL generated successfully for GCS object");

                return Map.of(
                    "success", true,
                    "bucketName", bucketName,
                    "objectName", objectName,
                    "signedUrl", signedUrl.toString(),
                    "expiresInMinutes", durationMinutes,
                    "expiresAt", LocalDateTime.now().plusMinutes(durationMinutes),
                    "generatedAt", LocalDateTime.now()
                );

            } catch (Exception e) {
                log.error("Failed to generate signed URL for GCS object", e);
                return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "bucketName", bucketName,
                    "objectName", objectName,
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * List objects in a bucket with optional prefix filter
     */
    public CompletableFuture<Map<String, Object>> listObjectsAsync(String bucketName, String prefix) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Listing objects in GCS bucket: bucket={}, prefix={}", bucketName, prefix);

                Storage.BlobListOption[] options = prefix != null && !prefix.isEmpty() 
                    ? new Storage.BlobListOption[]{Storage.BlobListOption.prefix(prefix)}
                    : new Storage.BlobListOption[0];

                Iterable<Blob> blobs = storage.list(bucketName, options).iterateAll();

                List<Map<String, Object>> objects = StreamSupport.stream(blobs.spliterator(), false)
                    .map(blob -> Map.of(
                        "name", blob.getName(),
                        "size", blob.getSize(),
                        "contentType", blob.getContentType() != null ? blob.getContentType() : "unknown",
                        "created", blob.getCreateTime(),
                        "updated", blob.getUpdateTime(),
                        "gsUri", String.format("gs://%s/%s", bucketName, blob.getName()),
                        "metadata", blob.getMetadata() != null ? blob.getMetadata() : Map.of()
                    ))
                    .collect(Collectors.toList());

                log.info("Listed {} objects from GCS bucket", objects.size());

                return Map.of(
                    "success", true,
                    "bucketName", bucketName,
                    "prefix", prefix != null ? prefix : "",
                    "objectCount", objects.size(),
                    "objects", objects,
                    "timestamp", LocalDateTime.now()
                );

            } catch (Exception e) {
                log.error("Failed to list objects from GCS bucket", e);
                return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "bucketName", bucketName,
                    "prefix", prefix,
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Copy object from one location to another within GCS
     */
    public CompletableFuture<Map<String, Object>> copyObjectAsync(String sourceBucket, String sourceObject, 
                                                                String targetBucket, String targetObject) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Copying object in GCS: from gs://{}/{} to gs://{}/{}", 
                        sourceBucket, sourceObject, targetBucket, targetObject);

                // Create target bucket if it doesn't exist
                createBucketIfNotExists(targetBucket);

                BlobId source = BlobId.of(sourceBucket, sourceObject);
                BlobId target = BlobId.of(targetBucket, targetObject);

                Storage.CopyRequest request = Storage.CopyRequest.newBuilder()
                    .setSource(source)
                    .setTarget(BlobInfo.newBuilder(target).build())
                    .build();

                CopyWriter copyWriter = storage.copy(request);
                Blob result = copyWriter.getResult();

                log.info("Object copied successfully in GCS");

                return Map.of(
                    "success", true,
                    "sourceBucket", sourceBucket,
                    "sourceObject", sourceObject,
                    "targetBucket", targetBucket,
                    "targetObject", targetObject,
                    "targetBlobId", result.getBlobId().toString(),
                    "size", result.getSize(),
                    "copyTime", LocalDateTime.now()
                );

            } catch (Exception e) {
                log.error("Failed to copy object in GCS", e);
                return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "sourceBucket", sourceBucket,
                    "sourceObject", sourceObject,
                    "targetBucket", targetBucket,
                    "targetObject", targetObject,
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Get object metadata and properties
     */
    public CompletableFuture<Map<String, Object>> getObjectPropertiesAsync(String bucketName, String objectName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Getting object properties from GCS: bucket={}, object={}", bucketName, objectName);

                BlobId blobId = BlobId.of(bucketName, objectName);
                Blob blob = storage.get(blobId);

                if (blob == null) {
                    return Map.of(
                        "success", false,
                        "error", "Object not found",
                        "bucketName", bucketName,
                        "objectName", objectName
                    );
                }

                Map<String, Object> blobProperties = new java.util.HashMap<>();
                blobProperties.put("success", true);
                blobProperties.put("bucketName", bucketName);
                blobProperties.put("objectName", objectName);
                blobProperties.put("blobId", blob.getBlobId().toString());
                blobProperties.put("size", blob.getSize());
                blobProperties.put("contentType", blob.getContentType());
                blobProperties.put("created", blob.getCreateTime());
                blobProperties.put("updated", blob.getUpdateTime());
                blobProperties.put("generation", blob.getGeneration());
                blobProperties.put("storageClass", blob.getStorageClass().toString());
                blobProperties.put("gsUri", String.format("gs://%s/%s", bucketName, objectName));
                blobProperties.put("publicUrl", getPublicUrl(bucketName, objectName));
                blobProperties.put("metadata", blob.getMetadata() != null ? blob.getMetadata() : Map.of());
                blobProperties.put("timestamp", LocalDateTime.now());
                return blobProperties;

            } catch (Exception e) {
                log.error("Failed to get object properties from GCS", e);
                return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "bucketName", bucketName,
                    "objectName", objectName,
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Delete object from Google Cloud Storage
     */
    public CompletableFuture<Map<String, Object>> deleteObjectAsync(String bucketName, String objectName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Deleting object from GCS: bucket={}, object={}", bucketName, objectName);

                BlobId blobId = BlobId.of(bucketName, objectName);
                boolean deleted = storage.delete(blobId);

                if (deleted) {
                    log.info("Object deleted successfully from GCS");
                    return Map.of(
                        "success", true,
                        "bucketName", bucketName,
                        "objectName", objectName,
                        "deleted", true,
                        "deleteTime", LocalDateTime.now()
                    );
                } else {
                    return Map.of(
                        "success", false,
                        "error", "Object not found or already deleted",
                        "bucketName", bucketName,
                        "objectName", objectName,
                        "deleted", false
                    );
                }

            } catch (Exception e) {
                log.error("Failed to delete object from GCS", e);
                return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "bucketName", bucketName,
                    "objectName", objectName,
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Health check for GCS service
     */
    public CompletableFuture<Map<String, Object>> checkHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Performing GCS health check");

                // Try to list buckets to verify connectivity
                Iterable<Bucket> buckets = storage.list(BucketListOption.pageSize(1)).iterateAll();
                Iterator<Bucket> iterator = buckets.iterator();
                boolean hasAccess = iterator.hasNext();

                Map<String, Object> health = new HashMap<>();
                health.put("status", "UP");
                health.put("service", "Google Cloud Storage");
                health.put("timestamp", LocalDateTime.now());
                health.put("hasAccess", hasAccess);
                health.put("serviceAvailable", true);

                log.debug("GCS health check completed successfully");
                return health;

            } catch (Exception e) {
                log.warn("GCS health check failed", e);
                return Map.of(
                    "status", "DOWN",
                    "service", "Google Cloud Storage",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now(),
                    "serviceAvailable", false
                );
            }
        });
    }

    // Helper methods

    private void createBucketIfNotExists(String bucketName) {
        try {
            Bucket bucket = storage.get(bucketName);
            if (bucket == null) {
                log.info("Creating GCS bucket: {}", bucketName);
                BucketInfo bucketInfo = BucketInfo.newBuilder(bucketName)
                        .setStorageClass(StorageClass.STANDARD)
                        .setLocation("us-central1")
                        .build();
                storage.create(bucketInfo);
                log.info("GCS bucket created successfully: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("Failed to create GCS bucket: {}", e.getMessage());
        }
    }

    private String getPublicUrl(String bucketName, String objectName) {
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
    }
}