package com.admin.service.azure;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.models.CategorizedEntityCollection;
import com.azure.ai.textanalytics.models.DetectedLanguage;
import com.azure.ai.textanalytics.models.DocumentSentiment;
import com.azure.ai.textanalytics.models.KeyPhrasesCollection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure Cognitive Services Integration
 * Provides machine learning and AI capabilities including text analytics, 
 * sentiment analysis, language detection, key phrase extraction, and OpenAI integration
 */
@Profile({"azure", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureCognitiveServicesService {

    private final TextAnalyticsClient textAnalyticsClient;
    private final OpenAIClient openAIClient;

    @Value("${azure.openai.deployment-name:gpt-35-turbo}")
    private String openAiDeploymentName;

    @Value("${azure.cognitive.language:en}")
    private String defaultLanguage;

    /**
     * Analyze sentiment of student feedback or course reviews
     */
    public CompletableFuture<Map<String, Object>> analyzeSentimentAsync(String text, String context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing sentiment for text: {} characters, context: {}", text.length(), context);

                DocumentSentiment sentiment = textAnalyticsClient.analyzeSentiment(text);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("context", context);
                result.put("textLength", text.length());
                result.put("overallSentiment", sentiment.getSentiment().toString());
                result.put("confidenceScores", Map.of(
                    "positive", sentiment.getConfidenceScores().getPositive(),
                    "neutral", sentiment.getConfidenceScores().getNeutral(),
                    "negative", sentiment.getConfidenceScores().getNegative()
                ));

                // Analyze sentence-level sentiment
                List<Map<String, Object>> sentences = sentiment.getSentences().stream()
                    .map(sentence -> Map.of(
                        "text", sentence.getText(),
                        "sentiment", sentence.getSentiment().toString(),
                        "confidenceScores", Map.of(
                            "positive", sentence.getConfidenceScores().getPositive(),
                            "neutral", sentence.getConfidenceScores().getNeutral(),
                            "negative", sentence.getConfidenceScores().getNegative()
                        )
                    )).collect(Collectors.toList());

                result.put("sentences", sentences);
                result.put("analysisTime", LocalDateTime.now());

                log.info("Sentiment analysis completed: overall={}, sentences={}", 
                    sentiment.getSentiment(), sentences.size());

                return result;

            } catch (Exception e) {
                log.error("Failed to analyze sentiment: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Extract key phrases from course descriptions or student essays
     */
    public CompletableFuture<Map<String, Object>> extractKeyPhrasesAsync(String text, String documentType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Extracting key phrases from {} text: {} characters", documentType, text.length());

                KeyPhrasesCollection keyPhrases = textAnalyticsClient.extractKeyPhrases(text);

                List<String> phrases = new ArrayList<>();
                for (String keyPhrase : keyPhrases) {
                    phrases.add(keyPhrase);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("documentType", documentType);
                result.put("textLength", text.length());
                result.put("keyPhrases", phrases);
                result.put("phraseCount", phrases.size());
                result.put("extractionTime", LocalDateTime.now());

                log.info("Key phrases extraction completed: {} phrases found", phrases.size());

                return result;

            } catch (Exception e) {
                log.error("Failed to extract key phrases: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Detect language of international student submissions
     */
    public CompletableFuture<Map<String, Object>> detectLanguageAsync(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Detecting language for text: {} characters", text.length());

                DetectedLanguage detectedLanguage = textAnalyticsClient.detectLanguage(text);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("textLength", text.length());
                result.put("detectedLanguage", detectedLanguage.getName());
                result.put("languageCode", detectedLanguage.getIso6391Name());
                result.put("confidenceScore", detectedLanguage.getConfidenceScore());
                result.put("detectionTime", LocalDateTime.now());

                log.info("Language detection completed: {} ({}), confidence: {}", 
                    detectedLanguage.getName(), detectedLanguage.getIso6391Name(), 
                    detectedLanguage.getConfidenceScore());

                return result;

            } catch (Exception e) {
                log.error("Failed to detect language: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Recognize named entities (people, locations, organizations) in academic content
     */
    public CompletableFuture<Map<String, Object>> recognizeEntitiesAsync(String text, String context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Recognizing entities in {} text: {} characters", context, text.length());

                CategorizedEntityCollection entities = textAnalyticsClient.recognizeEntities(text);

                List<Map<String, Object>> entityList = entities.stream()
                    .map(entity -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("text", entity.getText());
                        m.put("category", entity.getCategory().toString());
                        m.put("subcategory", entity.getSubcategory() != null ? entity.getSubcategory() : "");
                        m.put("confidenceScore", entity.getConfidenceScore());
                        m.put("offset", entity.getOffset());
                        m.put("length", entity.getLength());
                        return m;
                    }).collect(Collectors.toList());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("context", context);
                result.put("textLength", text.length());
                result.put("entities", entityList);
                result.put("entityCount", entityList.size());
                result.put("recognitionTime", LocalDateTime.now());

                log.info("Entity recognition completed: {} entities found", entityList.size());

                return result;

            } catch (Exception e) {
                log.error("Failed to recognize entities: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Generate course recommendations using OpenAI
     */
    public CompletableFuture<Map<String, Object>> generateCourseRecommendationsAsync(String studentProfile, String preferences) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating course recommendations for student profile");

                String systemPrompt = "You are an academic advisor for a college. Generate personalized course " +
                    "recommendations based on student profile and preferences. Provide exactly 5 recommended courses " +
                    "with explanations. Format as JSON with course name, code, description, and reason for recommendation.";

                String userPrompt = String.format(
                    "Student Profile: %s\nPreferences: %s\n\nPlease recommend 5 courses.",
                    studentProfile, preferences
                );

                List<ChatRequestMessage> messages = Arrays.asList(
                    new ChatRequestSystemMessage(systemPrompt),
                    new ChatRequestUserMessage(userPrompt)
                );

                ChatCompletionsOptions options = new ChatCompletionsOptions(messages)
                    .setMaxTokens(1000)
                    .setTemperature(0.7);

                ChatCompletions chatCompletions = openAIClient.getChatCompletions(openAiDeploymentName, options);

                String recommendations = chatCompletions.getChoices().get(0).getMessage().getContent();

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("studentProfile", studentProfile);
                result.put("preferences", preferences);
                result.put("recommendations", recommendations);
                result.put("model", openAiDeploymentName);
                result.put("generationTime", LocalDateTime.now());
                result.put("usage", Map.of(
                    "promptTokens", chatCompletions.getUsage().getPromptTokens(),
                    "completionTokens", chatCompletions.getUsage().getCompletionTokens(),
                    "totalTokens", chatCompletions.getUsage().getTotalTokens()
                ));

                log.info("Course recommendations generated successfully");

                return result;

            } catch (Exception e) {
                log.error("Failed to generate course recommendations: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Generate automated essay feedback using OpenAI
     */
    public CompletableFuture<Map<String, Object>> generateEssayFeedbackAsync(String essay, String assignment, String criteria) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating essay feedback for assignment: {}", assignment);

                String systemPrompt = "You are an experienced college instructor providing constructive feedback " +
                    "on student essays. Analyze the essay based on the given criteria and provide detailed, " +
                    "helpful feedback including strengths, areas for improvement, and specific suggestions.";

                String userPrompt = String.format(
                    "Assignment: %s\nEvaluation Criteria: %s\n\nEssay to Review:\n%s\n\n" +
                    "Please provide comprehensive feedback including a grade suggestion (A-F).",
                    assignment, criteria, essay
                );

                List<ChatRequestMessage> messages = Arrays.asList(
                    new ChatRequestSystemMessage(systemPrompt),
                    new ChatRequestUserMessage(userPrompt)
                );

                ChatCompletionsOptions options = new ChatCompletionsOptions(messages)
                    .setMaxTokens(1500)
                    .setTemperature(0.3);

                ChatCompletions chatCompletions = openAIClient.getChatCompletions(openAiDeploymentName, options);

                String feedback = chatCompletions.getChoices().get(0).getMessage().getContent();

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("assignment", assignment);
                result.put("criteria", criteria);
                result.put("essayLength", essay.length());
                result.put("feedback", feedback);
                result.put("model", openAiDeploymentName);
                result.put("feedbackTime", LocalDateTime.now());
                result.put("usage", Map.of(
                    "promptTokens", chatCompletions.getUsage().getPromptTokens(),
                    "completionTokens", chatCompletions.getUsage().getCompletionTokens(),
                    "totalTokens", chatCompletions.getUsage().getTotalTokens()
                ));

                log.info("Essay feedback generated successfully");

                return result;

            } catch (Exception e) {
                log.error("Failed to generate essay feedback: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Batch analyze multiple student submissions
     */
    public CompletableFuture<Map<String, Object>> batchAnalyzeSubmissionsAsync(List<String> submissions, String analysisType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Batch analyzing {} submissions for {}", submissions.size(), analysisType);

                List<Map<String, Object>> results = new ArrayList<>();

                for (int i = 0; i < submissions.size(); i++) {
                    String submission = submissions.get(i);
                    Map<String, Object> submissionResult = new HashMap<>();
                    submissionResult.put("index", i);
                    submissionResult.put("textLength", submission.length());

                    try {
                        switch (analysisType.toLowerCase()) {
                            case "sentiment":
                                DocumentSentiment sentiment = textAnalyticsClient.analyzeSentiment(submission);
                                submissionResult.put("sentiment", sentiment.getSentiment().toString());
                                submissionResult.put("confidenceScores", sentiment.getConfidenceScores());
                                break;

                            case "keyphrases":
                                KeyPhrasesCollection keyPhrases = textAnalyticsClient.extractKeyPhrases(submission);
                                List<String> phrases = new ArrayList<>();
                                for (String keyPhrase : keyPhrases) {
                                    phrases.add(keyPhrase);
                                }
                                submissionResult.put("keyPhrases", phrases);
                                break;

                            case "language":
                                DetectedLanguage language = textAnalyticsClient.detectLanguage(submission);
                                submissionResult.put("language", language.getName());
                                submissionResult.put("languageCode", language.getIso6391Name());
                                submissionResult.put("confidence", language.getConfidenceScore());
                                break;

                            default:
                                submissionResult.put("error", "Unsupported analysis type: " + analysisType);
                        }

                        submissionResult.put("success", true);

                    } catch (Exception e) {
                        submissionResult.put("success", false);
                        submissionResult.put("error", e.getMessage());
                    }

                    results.add(submissionResult);
                }

                long successCount = results.stream()
                    .mapToLong(r -> (Boolean) r.get("success") ? 1 : 0)
                    .sum();

                Map<String, Object> batchResult = new HashMap<>();
                batchResult.put("success", true);
                batchResult.put("analysisType", analysisType);
                batchResult.put("totalSubmissions", submissions.size());
                batchResult.put("successfulAnalyses", successCount);
                batchResult.put("failedAnalyses", submissions.size() - successCount);
                batchResult.put("results", results);
                batchResult.put("batchTime", LocalDateTime.now());

                log.info("Batch analysis completed: {}/{} successful analyses", successCount, submissions.size());

                return batchResult;

            } catch (Exception e) {
                log.error("Failed to perform batch analysis: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Health check for Azure Cognitive Services
     */
    public CompletableFuture<Map<String, Object>> checkHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> health = new HashMap<>();
            try {
                // Test text analytics with a simple operation
                textAnalyticsClient.detectLanguage("test");
                
                health.put("service", "Azure Cognitive Services");
                health.put("status", "UP");
                health.put("timestamp", LocalDateTime.now());
                health.put("textAnalytics", "Connected");
                health.put("openAI", "Available");
                health.put("defaultLanguage", defaultLanguage);
                health.put("openAiModel", openAiDeploymentName);

                log.debug("Azure Cognitive Services health check passed");

            } catch (Exception e) {
                health.put("service", "Azure Cognitive Services");
                health.put("status", "DOWN");
                health.put("error", e.getMessage());
                health.put("timestamp", LocalDateTime.now());

                log.error("Azure Cognitive Services health check failed: {}", e.getMessage());
            }
            return health;
        });
    }
}