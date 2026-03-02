package com.admin.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for cloud storage operations
 * Contains information about uploaded/downloaded files from AWS S3 or Azure Blob Storage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudStorageResponse {
    private String fileName;
    private String fileUrl;
    private String provider;
    private LocalDateTime uploadTime;
    private Long fileSize;
    private boolean success;
    private String errorMessage;
}