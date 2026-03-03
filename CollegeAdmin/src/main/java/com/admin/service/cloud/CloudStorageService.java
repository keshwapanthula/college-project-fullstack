package com.admin.service.cloud;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.multipart.MultipartFile;

import com.admin.dto.CloudStorageResponse;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Cloud Storage Service providing hybrid AWS S3 and Azure Blob Storage capabilities
 * Demonstrates multi-cloud architecture patterns for enterprise applications
 */
@Profile({"aws", "azure", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudStorageService {

    private final S3Client s3Client;
    private final BlobServiceClient blobServiceClient;

    @Value("${aws.s3.bucket-name:college-admin-bucket}")
    private String s3BucketName;

    @Value("${azure.storage.container-name:college-admin-container}")
    private String azureContainerName;

    @Value("${cloud.storage.primary-provider:aws}")
    private String primaryProvider;

    /**
     * Upload file to primary cloud provider with automatic failover
     */
    public CloudStorageResponse uploadFile(MultipartFile file, String directoryPath) {
        String fileName = generateFileName(file.getOriginalFilename(), directoryPath);
        
        try {
            if ("aws".equalsIgnoreCase(primaryProvider)) {
                return uploadToS3(file, fileName);
            } else {
                return uploadToAzure(file, fileName);
            }
        } catch (Exception e) {
            log.error("Primary storage failed, attempting failover: {}", e.getMessage());
            // Automatic failover to secondary provider
            try {
                if ("aws".equalsIgnoreCase(primaryProvider)) {
                    return uploadToAzure(file, fileName);
                } else {
                    return uploadToS3(file, fileName);
                }
            } catch (Exception failoverException) {
                log.error("Failover storage also failed: {}", failoverException.getMessage());
                throw new RuntimeException("Both storage providers failed", failoverException);
            }
        }
    }

    /**
     * Upload file to AWS S3
     */
    private CloudStorageResponse uploadToS3(MultipartFile file, String fileName) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = String.format("https://%s.s3.amazonaws.com/%s", s3BucketName, fileName);
            
            log.info("File uploaded to S3: {}", fileName);
            
            return CloudStorageResponse.builder()
                    .fileName(fileName)
                    .fileUrl(fileUrl)
                    .provider("AWS S3")
                    .uploadTime(LocalDateTime.now())
                    .fileSize(file.getSize())
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("S3 upload failed: {}", e.getMessage());
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    /**
     * Upload file to Azure Blob Storage
     */
    private CloudStorageResponse uploadToAzure(MultipartFile file, String fileName) {
        try {
            BlobClient blobClient = blobServiceClient
                    .getBlobContainerClient(azureContainerName)
                    .getBlobClient(fileName);

            blobClient.upload(new ByteArrayInputStream(file.getBytes()), file.getSize(), true);

            String fileUrl = blobClient.getBlobUrl();
            
            log.info("File uploaded to Azure Blob Storage: {}", fileName);
            
            return CloudStorageResponse.builder()
                    .fileName(fileName)
                    .fileUrl(fileUrl)
                    .provider("Azure Blob Storage")
                    .uploadTime(LocalDateTime.now())
                    .fileSize(file.getSize())
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Azure Blob upload failed: {}", e.getMessage());
            throw new RuntimeException("Azure Blob upload failed", e);
        }
    }

    /**
     * Download file from both providers based on file URL
     */
    public InputStream downloadFile(String fileName) {
        try {
            // Try primary provider first
            if ("aws".equalsIgnoreCase(primaryProvider)) {
                return downloadFromS3(fileName);
            } else {
                return downloadFromAzure(fileName);
            }
        } catch (Exception e) {
            log.error("Primary download failed, trying secondary provider: {}", e.getMessage());
            // Try secondary provider
            try {
                if ("aws".equalsIgnoreCase(primaryProvider)) {
                    return downloadFromAzure(fileName);
                } else {
                    return downloadFromS3(fileName);
                }
            } catch (Exception failoverException) {
                log.error("Both download providers failed: {}", failoverException.getMessage());
                throw new RuntimeException("File not found in any storage provider", failoverException);
            }
        }
    }

    private InputStream downloadFromS3(String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3BucketName)
                .key(fileName)
                .build();
        
        return s3Client.getObject(getObjectRequest);
    }

    private InputStream downloadFromAzure(String fileName) {
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(azureContainerName)
                .getBlobClient(fileName);
        
        return blobClient.openInputStream();
    }

    private String generateFileName(String originalFileName, String directoryPath) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String uniqueId = UUID.randomUUID().toString();
        String sanitizedPath = directoryPath != null ? directoryPath.replaceAll("[^a-zA-Z0-9/]", "") : "default";
        
        return String.format("%s/%s_%s%s", sanitizedPath, "college_admin", uniqueId, extension);
    }
}