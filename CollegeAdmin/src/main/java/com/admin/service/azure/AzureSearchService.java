package com.admin.service.azure;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.IndexDocumentsResult;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.models.SuggestOptions;
import com.azure.search.documents.models.SuggestResult;
import com.azure.search.documents.util.SearchPagedIterable;
import com.azure.search.documents.util.SuggestPagedIterable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure Cognitive Search Service
 * Provides comprehensive search capabilities including document indexing,
 * full-text search, faceted search, and analytics
 */
@Profile({"azure", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureSearchService {

    private final SearchClient searchClient;

    @Value("${azure.search.index-name:students-index}")
    private String indexName;

    /**
     * Index student document for search
     */
    public CompletableFuture<Map<String, Object>> indexStudentDocumentAsync(String studentId, String name, 
                                                                           String email, String program, 
                                                                           List<String> courses, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Indexing student document: {} - {}", studentId, name);

                SearchDocument document = new SearchDocument();
                document.put("studentId", studentId);
                document.put("name", name);
                document.put("email", email);
                document.put("program", program);
                document.put("courses", courses);
                document.put("enrollmentDate", LocalDateTime.now().toString());
                document.put("isActive", true);
                document.put("searchableText", name + " " + email + " " + program + " " + String.join(" ", courses));
                
                if (metadata != null) {
                    document.putAll(metadata);
                }

                IndexDocumentsResult result = searchClient.uploadDocuments(Collections.singletonList(document));

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("studentId", studentId);
                response.put("indexName", indexName);
                response.put("documentKey", studentId);
                response.put("indexedFields", document.keySet());
                response.put("indexTime", LocalDateTime.now());
                response.put("statusCode", result.getResults().get(0).getStatusCode());

                log.info("Student document indexed successfully: {}", studentId);
                return response;

            } catch (Exception e) {
                log.error("Failed to index student document: {}", e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", e.getMessage());
                return response;
            }
        });
    }

    /**
     * Search students with various criteria
     */
    public CompletableFuture<Map<String, Object>> searchStudentsAsync(String query, String program, 
                                                                      boolean includeInactive, int top, int skip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Searching students: query='{}', program='{}', top={}", query, program, top);

                SearchOptions options = new SearchOptions()
                    .setTop(top)
                    .setSkip(skip)
                    .setIncludeTotalCount(true)
                    .setHighlightFields("name", "searchableText")
                    .setFacets("program", "isActive")
                    .setSelect("studentId", "name", "email", "program", "courses", "enrollmentDate", "isActive");

                // Build search filter
                List<String> filters = new ArrayList<>();
                if (program != null && !program.isEmpty()) {
                    filters.add("program eq '" + program + "'");
                }
                if (!includeInactive) {
                    filters.add("isActive eq true");
                }
                
                if (!filters.isEmpty()) {
                    options.setFilter(String.join(" and ", filters));
                }

                SearchPagedIterable searchResults = searchClient.search(query, options, null);

                List<Map<String, Object>> students = new ArrayList<>();
                for (SearchResult searchResult : searchResults) {
                    SearchDocument document = searchResult.getDocument(SearchDocument.class);
                    Map<String, Object> student = new HashMap<>();
                    student.put("studentId", document.get("studentId"));
                    student.put("name", document.get("name"));
                    student.put("email", document.get("email"));
                    student.put("program", document.get("program"));
                    student.put("courses", document.get("courses"));
                    student.put("score", searchResult.getScore());
                    student.put("highlights", searchResult.getHighlights());
                    students.add(student);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("query", query);
                response.put("totalCount", searchResults.getTotalCount());
                response.put("returnedCount", students.size());
                response.put("students", students);
                response.put("facets", searchResults.getFacets());
                response.put("searchTime", LocalDateTime.now());

                log.info("Student search completed: {} results found", students.size());
                return response;

            } catch (Exception e) {
                log.error("Failed to search students: {}", e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", e.getMessage());
                return response;
            }
        });
    }

    /**
     * Suggest search terms (autocomplete)
     */
    public CompletableFuture<Map<String, Object>> suggestSearchTermsAsync(String partialQuery, String suggesterName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Getting search suggestions for: '{}'", partialQuery);

                SuggestOptions options = new SuggestOptions()
                    .setUseFuzzyMatching(true)
                    .setTop(10)
                    .setSelect("name", "program", "email");

                SuggestPagedIterable suggestResults = searchClient.suggest(partialQuery, "student-suggester", options, null);

                List<Map<String, Object>> suggestions = new ArrayList<>();
                for (SuggestResult suggestResult : suggestResults) {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("text", suggestResult.getText());
                    suggestion.put("document", suggestResult.getDocument(SearchDocument.class));
                    suggestions.add(suggestion);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("partialQuery", partialQuery);
                response.put("suggestionCount", suggestions.size());
                response.put("suggestions", suggestions);
                response.put("suggestionTime", LocalDateTime.now());

                log.info("Search suggestions completed: {} suggestions found", suggestions.size());
                return response;

            } catch (Exception e) {
                log.error("Failed to get search suggestions: {}", e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", e.getMessage());
                return response;
            }
        });
    }

    /**
     * Perform analytics on search data
     */
    public CompletableFuture<Map<String, Object>> performSearchAnalyticsAsync(String dateRange) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Performing search analytics for date range: {}", dateRange);

                // Get faceted search data for analytics
                SearchOptions options = new SearchOptions()
                    .setTop(0)
                    .setFacets("program", "isActive", "enrollmentDate")
                    .setIncludeTotalCount(true);

                SearchPagedIterable searchResults = searchClient.search("*", options, null);

                Map<String, Object> analytics = new HashMap<>();
                analytics.put("totalDocuments", searchResults.getTotalCount());
                analytics.put("facets", searchResults.getFacets());

                // Mock additional analytics data
                analytics.put("topSearchQueries", Arrays.asList(
                    Map.of("query", "computer science", "count", 150),
                    Map.of("query", "mathematics", "count", 120),
                    Map.of("query", "engineering", "count", 98)
                ));
                
                analytics.put("searchVolumeTrends", Map.of(
                    "daily", Math.random() * 1000,
                    "weekly", Math.random() * 7000,
                    "monthly", Math.random() * 30000
                ));

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("dateRange", dateRange);
                response.put("analytics", analytics);
                response.put("generatedTime", LocalDateTime.now());

                log.info("Search analytics completed for date range: {}", dateRange);
                return response;

            } catch (Exception e) {
                log.error("Failed to perform search analytics: {}", e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", e.getMessage());
                return response;
            }
        });
    }

    /**
     * Delete student document from search index
     */
    public CompletableFuture<Map<String, Object>> deleteStudentDocumentAsync(String studentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Deleting student document from search index: {}", studentId);

                SearchDocument document = new SearchDocument();
                document.put("studentId", studentId);

                IndexDocumentsResult result = searchClient.deleteDocuments(Collections.singletonList(document));

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("studentId", studentId);
                response.put("indexName", indexName);
                response.put("deleteTime", LocalDateTime.now());
                response.put("statusCode", result.getResults().get(0).getStatusCode());

                log.info("Student document deleted successfully from search index: {}", studentId);
                return response;

            } catch (Exception e) {
                log.error("Failed to delete student document: {}", e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", e.getMessage());
                return response;
            }
        });
    }

    /**
     * Health check for Azure Search service
     */
    public CompletableFuture<Map<String, Object>> checkHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> health = new HashMap<>();
            try {
                // Test with a simple search
                SearchOptions options = new SearchOptions().setTop(1);
                SearchPagedIterable searchResults = searchClient.search("*", options, null);
                
                health.put("service", "Azure Cognitive Search");
                health.put("status", "UP");
                health.put("timestamp", LocalDateTime.now());
                health.put("indexName", indexName);
                health.put("documentsIndexed", searchResults.getTotalCount());

                log.debug("Azure Search health check passed");

            } catch (Exception e) {
                health.put("service", "Azure Cognitive Search");
                health.put("status", "DOWN");
                health.put("error", e.getMessage());
                health.put("timestamp", LocalDateTime.now());

                log.error("Azure Search health check failed: {}", e.getMessage());
            }
            return health;
        });
    }
}