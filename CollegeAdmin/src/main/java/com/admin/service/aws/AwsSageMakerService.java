package com.admin.service.aws;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;
import software.amazon.awssdk.services.sagemaker.model.AlgorithmSpecification;
import software.amazon.awssdk.services.sagemaker.model.CompressionType;
import software.amazon.awssdk.services.sagemaker.model.CreateTrainingJobRequest;
import software.amazon.awssdk.services.sagemaker.model.CreateTrainingJobResponse;
import software.amazon.awssdk.services.sagemaker.model.DataSource;
import software.amazon.awssdk.services.sagemaker.model.DescribeTrainingJobRequest;
import software.amazon.awssdk.services.sagemaker.model.DescribeTrainingJobResponse;
import software.amazon.awssdk.services.sagemaker.model.ListModelsRequest;
import software.amazon.awssdk.services.sagemaker.model.ListModelsResponse;
import software.amazon.awssdk.services.sagemaker.model.OutputDataConfig;
import software.amazon.awssdk.services.sagemaker.model.ResourceConfig;
import software.amazon.awssdk.services.sagemaker.model.S3DataDistribution;
import software.amazon.awssdk.services.sagemaker.model.S3DataSource;
import software.amazon.awssdk.services.sagemaker.model.S3DataType;
import software.amazon.awssdk.services.sagemaker.model.StoppingCondition;
import software.amazon.awssdk.services.sagemaker.model.TrainingInputMode;
import software.amazon.awssdk.services.sagemaker.model.TrainingInstanceType;

/**
 * AWS SageMaker Machine Learning Service
 * Provides ML model training, deployment, and inference capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsSageMakerService {

    private final SageMakerClient sageMakerClient;

    @Value("${aws.sagemaker.endpoint-name:student-sentiment-analyzer}")
    private String sentimentEndpointName;

    @Value("${aws.sagemaker.model-name:student-recommendation-model}")
    private String recommendationModelName;

    @Value("${aws.sagemaker.training-job-prefix:college-ml-training}")
    private String trainingJobPrefix;

    /**
     * Analyze student feedback sentiment using ML model
     */
    public CompletableFuture<SentimentAnalysisResult> analyzeSentiment(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing sentiment for text: {}", text.substring(0, Math.min(text.length(), 50)) + "...");

                // Invoke SageMaker endpoint for sentiment analysis (simulated for demo)
                // Real implementation would use SageMakerRuntimeClient.invokeEndpoint()

                // Note: This would require a deployed SageMaker endpoint
                // For demo purposes, we'll simulate the response
                SentimentAnalysisResult result = SentimentAnalysisResult.builder()
                        .sentiment(determineSentiment(text))
                        .confidence(0.85 + Math.random() * 0.1)
                        .text(text)
                        .modelVersion("v1.0")
                        .build();

                log.info("Sentiment analysis completed: {} with confidence {}", result.getSentiment(), result.getConfidence());
                return result;

            } catch (Exception e) {
                log.error("Error analyzing sentiment", e);
                throw new RuntimeException("Failed to analyze sentiment", e);
            }
        });
    }

    /**
     * Generate course recommendations for students using ML model
     */
    public CompletableFuture<List<CourseRecommendation>> generateCourseRecommendations(StudentProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating course recommendations for student: {}", profile.getStudentId());

                // Create model input from student profile
                String modelInput = String.format("""
                    {
                        "student_id": "%s",
                        "department": "%s",
                        "gpa": %.2f,
                        "completed_courses": %s,
                        "interests": %s
                    }
                    """, profile.getStudentId(), profile.getDepartment(), 
                         profile.getGpa(), profile.getCompletedCourses(), profile.getInterests());

                // For demo purposes, generate mock recommendations
                List<CourseRecommendation> recommendations = List.of(
                    CourseRecommendation.builder()
                            .courseId("CS401")
                            .courseName("Advanced Machine Learning")
                            .confidence(0.92)
                            .reason("Strong performance in prerequisite courses")
                            .estimatedDifficulty("High")
                            .build(),
                    CourseRecommendation.builder()
                            .courseId("MATH301")
                            .courseName("Statistical Analysis")
                            .confidence(0.88)
                            .reason("Complements current coursework")
                            .estimatedDifficulty("Medium")
                            .build(),
                    CourseRecommendation.builder()
                            .courseId("CS350")
                            .courseName("Data Structures Advanced")
                            .confidence(0.85)
                            .reason("Natural progression from current courses")
                            .estimatedDifficulty("Medium")
                            .build()
                );

                log.info("Generated {} course recommendations", recommendations.size());
                return recommendations;

            } catch (Exception e) {
                log.error("Error generating course recommendations", e);
                throw new RuntimeException("Failed to generate recommendations", e);
            }
        });
    }

    /**
     * Start ML model training job
     */
    public CompletableFuture<TrainingJobResult> startTrainingJob(TrainingJobConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jobName = trainingJobPrefix + "-" + System.currentTimeMillis();
                
                log.info("Starting SageMaker training job: {}", jobName);

                CreateTrainingJobRequest request = CreateTrainingJobRequest.builder()
                        .trainingJobName(jobName)
                        .algorithmSpecification(AlgorithmSpecification.builder()
                                .trainingImage("382416733822.dkr.ecr.us-east-1.amazonaws.com/scikit-learn:0.23-1-cpu-py3")
                                .trainingInputMode(TrainingInputMode.FILE)
                                .build())
                        .roleArn(config.getRoleArn())
                        .inputDataConfig(software.amazon.awssdk.services.sagemaker.model.Channel.builder()
                                .channelName("training")
                                .dataSource(DataSource.builder()
                                        .s3DataSource(S3DataSource.builder()
                                                .s3DataType(S3DataType.S3_PREFIX)
                                                .s3Uri(config.getTrainingDataPath())
                                                .s3DataDistributionType(S3DataDistribution.FULLY_REPLICATED)
                                                .build())
                                        .build())
                                .contentType("text/csv")
                                .compressionType(CompressionType.NONE)
                                .build())
                        .outputDataConfig(OutputDataConfig.builder()
                                .s3OutputPath(config.getOutputPath())
                                .build())
                        .resourceConfig(ResourceConfig.builder()
                                .instanceType(TrainingInstanceType.ML_M5_LARGE)
                                .instanceCount(1)
                                .volumeSizeInGB(30)
                                .build())
                        .stoppingCondition(StoppingCondition.builder()
                                .maxRuntimeInSeconds(3600) // 1 hour
                                .build())
                        .build();

                CreateTrainingJobResponse response = sageMakerClient.createTrainingJob(request);

                TrainingJobResult result = TrainingJobResult.builder()
                        .jobName(jobName)
                        .jobArn(response.trainingJobArn())
                        .status("InProgress")
                        .createdAt(java.time.Instant.now().toString())
                        .build();

                log.info("Training job started successfully: {}", jobName);
                return result;

            } catch (Exception e) {
                log.error("Error starting training job", e);
                throw new RuntimeException("Failed to start training job", e);
            }
        });
    }

    /**
     * Get training job status
     */
    public CompletableFuture<TrainingJobStatus> getTrainingJobStatus(String jobName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Checking training job status: {}", jobName);

                DescribeTrainingJobRequest request = DescribeTrainingJobRequest.builder()
                        .trainingJobName(jobName)
                        .build();

                DescribeTrainingJobResponse response = sageMakerClient.describeTrainingJob(request);

                TrainingJobStatus status = TrainingJobStatus.builder()
                        .jobName(jobName)
                        .status(response.trainingJobStatus().toString())
                        .createdAt(response.creationTime().toString())
                        .startedAt(response.trainingStartTime() != null ? response.trainingStartTime().toString() : null)
                        .completedAt(response.trainingEndTime() != null ? response.trainingEndTime().toString() : null)
                        .failureReason(response.failureReason())
                        .build();

                log.info("Training job {} status: {}", jobName, status.getStatus());
                return status;

            } catch (Exception e) {
                log.error("Error getting training job status", e);
                throw new RuntimeException("Failed to get training job status", e);
            }
        });
    }

    /**
     * List available ML models
     */
    public CompletableFuture<List<ModelInfo>> listModels() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Listing SageMaker models");

                ListModelsRequest request = ListModelsRequest.builder()
                        .maxResults(10)
                        .build();

                ListModelsResponse response = sageMakerClient.listModels(request);

                List<ModelInfo> models = response.models().stream()
                        .map(model -> ModelInfo.builder()
                                .modelName(model.modelName())
                                .modelArn(model.modelArn())
                                .createdAt(model.creationTime().toString())
                                .build())
                        .toList();

                log.info("Found {} models", models.size());
                return models;

            } catch (Exception e) {
                log.error("Error listing models", e);
                throw new RuntimeException("Failed to list models", e);
            }
        });
    }

    // Helper method to simulate sentiment analysis
    private String determineSentiment(String text) {
        String lowerText = text.toLowerCase();
        
        // Simple keyword-based sentiment analysis for demo
        if (lowerText.contains("excellent") || lowerText.contains("great") || 
            lowerText.contains("good") || lowerText.contains("love")) {
            return "POSITIVE";
        } else if (lowerText.contains("terrible") || lowerText.contains("bad") || 
                   lowerText.contains("hate") || lowerText.contains("awful")) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * Get SageMaker service health status
     */
    public CompletableFuture<Map<String, Object>> getSageMakerHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test connection by listing models
                ListModelsRequest request = ListModelsRequest.builder()
                        .maxResults(1)
                        .build();
                
                sageMakerClient.listModels(request);

                return Map.of(
                        "status", "UP",
                        "service", "AWS SageMaker",
                        "timestamp", java.time.Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("SageMaker health check failed", e);
                return Map.of(
                        "status", "DOWN",
                        "error", e.getMessage(),
                        "timestamp", java.time.Instant.now().toString()
                );
            }
        });
    }

    // Data Transfer Objects
    @Data
    @lombok.Builder
    public static class SentimentAnalysisResult {
        private String sentiment;
        private double confidence;
        private String text;
        private String modelVersion;
    }

    @Data
    @lombok.Builder
    public static class CourseRecommendation {
        private String courseId;
        private String courseName;
        private double confidence;
        private String reason;
        private String estimatedDifficulty;
    }

    @Data
    @lombok.Builder
    public static class StudentProfile {
        private String studentId;
        private String department;
        private double gpa;
        private List<String> completedCourses;
        private List<String> interests;
    }

    @Data
    @lombok.Builder
    public static class TrainingJobConfig {
        private String roleArn;
        private String trainingDataPath;
        private String outputPath;
        private String algorithmName;
    }

    @Data
    @lombok.Builder
    public static class TrainingJobResult {
        private String jobName;
        private String jobArn;
        private String status;
        private String createdAt;
    }

    @Data
    @lombok.Builder
    public static class TrainingJobStatus {
        private String jobName;
        private String status;
        private String createdAt;
        private String startedAt;
        private String completedAt;
        private String failureReason;
    }

    @Data
    @lombok.Builder
    public static class ModelInfo {
        private String modelName;
        private String modelArn;
        private String createdAt;
    }
}