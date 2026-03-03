package com.admin.service.azure;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure Event Grid Service
 * Provides event-driven architecture capabilities for microservices communication
 * including event publishing, custom topics, and event routing
 */
@Profile({"azure", "default"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureEventGridService {

    private final EventGridPublisherClient<EventGridEvent> eventGridPublisherClient;

    @Value("${azure.eventgrid.topic-name:college-admin-topic}")
    private String defaultTopicName;

    @Value("${azure.eventgrid.source:college-admin-service}")
    private String eventSource;

    /**
     * Publish student enrollment event
     */
    public CompletableFuture<Map<String, Object>> publishStudentEnrollmentEventAsync(String studentId, String courseId, 
                                                                                   String action, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Publishing student enrollment event: {} - {} in course {}", action, studentId, courseId);

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("studentId", studentId);
                eventData.put("courseId", courseId);
                eventData.put("action", action);
                eventData.put("timestamp", LocalDateTime.now());
                eventData.put("metadata", metadata);

                EventGridEvent event = new EventGridEvent(
                    eventSource,
                    "Student.Enrollment." + action,
                    BinaryData.fromObject(eventData),
                    "1.0"
                );
                event.setEventTime(OffsetDateTime.now());

                eventGridPublisherClient.sendEvent(event);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("eventType", "Student.Enrollment." + action);
                result.put("studentId", studentId);
                result.put("courseId", courseId);
                result.put("action", action);
                result.put("eventId", event.getId());
                result.put("publishTime", LocalDateTime.now());
                result.put("topic", defaultTopicName);

                log.info("Student enrollment event published successfully: {}", event.getId());
                return result;

            } catch (Exception e) {
                log.error("Failed to publish student enrollment event: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Publish course update event
     */
    public CompletableFuture<Map<String, Object>> publishCourseUpdateEventAsync(String courseId, String updateType, 
                                                                               Object courseData, String updatedBy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Publishing course update event: {} for course {}", updateType, courseId);

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("courseId", courseId);
                eventData.put("updateType", updateType);
                eventData.put("courseData", courseData);
                eventData.put("updatedBy", updatedBy);
                eventData.put("updateTimestamp", LocalDateTime.now());

                EventGridEvent event = new EventGridEvent(
                    eventSource,
                    "Course.Update." + updateType,
                    BinaryData.fromObject(eventData),
                    "1.0"
                );
                event.setEventTime(OffsetDateTime.now());

                eventGridPublisherClient.sendEvent(event);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("eventType", "Course.Update." + updateType);
                result.put("courseId", courseId);
                result.put("updateType", updateType);
                result.put("updatedBy", updatedBy);
                result.put("eventId", event.getId());
                result.put("publishTime", LocalDateTime.now());

                log.info("Course update event published successfully: {}", event.getId());
                return result;

            } catch (Exception e) {
                log.error("Failed to publish course update event: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Publish admin notification event
     */
    public CompletableFuture<Map<String, Object>> publishAdminNotificationEventAsync(String notificationType, String message, 
                                                                                    String priority, Map<String, Object> details) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Publishing admin notification event: {} - {}", notificationType, priority);

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("notificationType", notificationType);
                eventData.put("message", message);
                eventData.put("priority", priority);
                eventData.put("details", details);
                eventData.put("timestamp", LocalDateTime.now());
                eventData.put("source", "college-admin-service");

                EventGridEvent event = new EventGridEvent(
                    eventSource,
                    "Admin.Notification." + notificationType,
                    BinaryData.fromObject(eventData),
                    "1.0"
                );
                event.setEventTime(OffsetDateTime.now());

                eventGridPublisherClient.sendEvent(event);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("eventType", "Admin.Notification." + notificationType);
                result.put("notificationType", notificationType);
                result.put("priority", priority);
                result.put("eventId", event.getId());
                result.put("publishTime", LocalDateTime.now());

                log.info("Admin notification event published successfully: {}", event.getId());
                return result;

            } catch (Exception e) {
                log.error("Failed to publish admin notification event: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Publish batch of events
     */
    public CompletableFuture<Map<String, Object>> publishBatchEventsAsync(List<Map<String, Object>> events) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Publishing batch of {} events", events.size());

                List<EventGridEvent> eventGridEvents = new ArrayList<>();
                List<String> eventIds = new ArrayList<>();

                for (Map<String, Object> eventInfo : events) {
                    EventGridEvent event = new EventGridEvent(
                        (String) eventInfo.getOrDefault("source", eventSource),
                        (String) eventInfo.get("eventType"),
                        BinaryData.fromObject(eventInfo.get("data")),
                        (String) eventInfo.getOrDefault("dataVersion", "1.0")
                    );
                    event.setEventTime(OffsetDateTime.now());

                    eventGridEvents.add(event);
                    eventIds.add(event.getId());
                }

                eventGridPublisherClient.sendEvents(eventGridEvents);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("eventCount", events.size());
                result.put("eventIds", eventIds);
                result.put("publishTime", LocalDateTime.now());
                result.put("topic", defaultTopicName);

                log.info("Batch events published successfully: {} events", events.size());
                return result;

            } catch (Exception e) {
                log.error("Failed to publish batch events: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Publish custom event with CloudEvent format
     */
    public CompletableFuture<Map<String, Object>> publishCloudEventAsync(String eventType, String subject,
                                                                        Object data, String contentType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Publishing event: {} - {}", eventType, subject);

                EventGridEvent event = new EventGridEvent(
                    subject,
                    eventType,
                    BinaryData.fromObject(data),
                    "1.0"
                );
                event.setEventTime(OffsetDateTime.now());

                eventGridPublisherClient.sendEvent(event);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("eventType", eventType);
                result.put("subject", subject);
                result.put("contentType", contentType);
                result.put("eventId", event.getId());
                result.put("publishTime", LocalDateTime.now());
                result.put("source", eventSource);

                log.info("Event published successfully: {}", event.getId());
                return result;

            } catch (Exception e) {
                log.error("Failed to publish CloudEvent: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Health check for Azure Event Grid service
     */
    public CompletableFuture<Map<String, Object>> checkHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> health = new HashMap<>();
            try {
                // Test with a simple ping event
                Map<String, Object> pingData = Map.of(
                    "service", "azure-event-grid",
                    "timestamp", LocalDateTime.now(),
                    "healthCheck", true
                );

                EventGridEvent pingEvent = new EventGridEvent(
                    eventSource,
                    "Health.Check",
                    BinaryData.fromObject(pingData),
                    "1.0"
                );
                pingEvent.setEventTime(OffsetDateTime.now());

                eventGridPublisherClient.sendEvent(pingEvent);

                health.put("service", "Azure Event Grid");
                health.put("status", "UP");
                health.put("timestamp", LocalDateTime.now());
                health.put("defaultTopic", defaultTopicName);
                health.put("eventSource", eventSource);

                log.debug("Azure Event Grid health check passed");

            } catch (Exception e) {
                health.put("service", "Azure Event Grid");
                health.put("status", "DOWN");
                health.put("error", e.getMessage());
                health.put("timestamp", LocalDateTime.now());

                log.error("Azure Event Grid health check failed: {}", e.getMessage());
            }
            return health;
        });
    }
}