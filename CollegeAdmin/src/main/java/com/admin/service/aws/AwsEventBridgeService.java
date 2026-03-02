package com.admin.service.aws;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.ListRulesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListRulesResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.PutRuleResponse;
import software.amazon.awssdk.services.eventbridge.model.PutTargetsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutTargetsResponse;
import software.amazon.awssdk.services.eventbridge.model.RuleState;
import software.amazon.awssdk.services.eventbridge.model.Target;

/**
 * AWS EventBridge Event-Driven Architecture Service
 * Provides serverless event routing and processing capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsEventBridgeService {

    private final EventBridgeClient eventBridgeClient;

    @Value("${aws.eventbridge.bus-name:college-events}")
    private String eventBusName;

    @Value("${aws.eventbridge.source:college.management}")
    private String eventSource;

    /**
     * Send custom event to EventBridge
     */
    public CompletableFuture<EventResult> sendEvent(CustomEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Sending event: {} to EventBridge", event.getEventType());

                PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                        .source(eventSource)
                        .detailType(event.getEventType())
                        .detail(event.getEventData())
                        .eventBusName(eventBusName)
                        .resources(event.getResources())
                        .build();

                PutEventsRequest request = PutEventsRequest.builder()
                        .entries(entry)
                        .build();

                PutEventsResponse response = eventBridgeClient.putEvents(request);

                EventResult result = EventResult.builder()
                        .eventId(response.entries().get(0).eventId())
                        .eventStatus("SUCCESS")
                        .failedEntryCount(response.failedEntryCount())
                        .timestamp(java.time.Instant.now().toString())
                        .build();

                log.info("Event sent successfully with ID: {}", result.getEventId());
                return result;

            } catch (Exception e) {
                log.error("Error sending event to EventBridge", e);
                throw new RuntimeException("Failed to send event", e);
            }
        });
    }

    /**
     * Send student enrollment event
     */
    public CompletableFuture<EventResult> sendStudentEnrollmentEvent(StudentEnrollmentEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Sending student enrollment event for student: {}", event.getStudentId());

                String eventDetail = String.format("""
                    {
                        "studentId": "%s",
                        "studentName": "%s",
                        "courseId": "%s",
                        "courseName": "%s",
                        "semester": "%s",
                        "enrollmentDate": "%s",
                        "department": "%s",
                        "enrollmentType": "%s"
                    }
                    """, event.getStudentId(), event.getStudentName(),
                         event.getCourseId(), event.getCourseName(),
                         event.getSemester(), event.getEnrollmentDate(),
                         event.getDepartment(), event.getEnrollmentType());

                CustomEvent customEvent = CustomEvent.builder()
                        .eventType("Student Enrollment")
                        .eventData(eventDetail)
                        .resources(List.of("student:" + event.getStudentId(), "course:" + event.getCourseId()))
                        .build();

                return sendEvent(customEvent).join();

            } catch (Exception e) {
                log.error("Error sending student enrollment event", e);
                throw new RuntimeException("Failed to send student enrollment event", e);
            }
        });
    }

    /**
     * Send grade update event
     */
    public CompletableFuture<EventResult> sendGradeUpdateEvent(GradeUpdateEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Sending grade update event for student: {}", event.getStudentId());

                String eventDetail = String.format("""
                    {
                        "studentId": "%s",
                        "courseId": "%s",
                        "assignmentId": "%s",
                        "oldGrade": "%.2f",
                        "newGrade": "%.2f",
                        "gradedBy": "%s",
                        "gradedAt": "%s",
                        "semester": "%s"
                    }
                    """, event.getStudentId(), event.getCourseId(),
                         event.getAssignmentId(), event.getOldGrade(),
                         event.getNewGrade(), event.getGradedBy(),
                         event.getGradedAt(), event.getSemester());

                CustomEvent customEvent = CustomEvent.builder()
                        .eventType("Grade Update")
                        .eventData(eventDetail)
                        .resources(List.of("student:" + event.getStudentId(), 
                                         "course:" + event.getCourseId(),
                                         "assignment:" + event.getAssignmentId()))
                        .build();

                return sendEvent(customEvent).join();

            } catch (Exception e) {
                log.error("Error sending grade update event", e);
                throw new RuntimeException("Failed to send grade update event", e);
            }
        });
    }

    /**
     * Create EventBridge rule
     */
    public CompletableFuture<RuleCreationResult> createEventRule(EventRuleConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating EventBridge rule: {}", config.getRuleName());

                PutRuleRequest request = PutRuleRequest.builder()
                        .name(config.getRuleName())
                        .description(config.getDescription())
                        .eventPattern(config.getEventPattern())
                        .state(RuleState.ENABLED)
                        .eventBusName(eventBusName)
                        .build();

                PutRuleResponse response = eventBridgeClient.putRule(request);

                RuleCreationResult result = RuleCreationResult.builder()
                        .ruleArn(response.ruleArn())
                        .ruleName(config.getRuleName())
                        .eventBusName(eventBusName)
                        .description(config.getDescription())
                        .state("ENABLED")
                        .build();

                log.info("EventBridge rule created successfully: {}", result.getRuleArn());
                return result;

            } catch (Exception e) {
                log.error("Error creating EventBridge rule", e);
                throw new RuntimeException("Failed to create EventBridge rule", e);
            }
        });
    }

    /**
     * Add target to EventBridge rule
     */
    public CompletableFuture<TargetResult> addRuleTarget(String ruleName, RuleTarget target) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Adding target to rule: {}", ruleName);

                Target eventTarget = Target.builder()
                        .id(target.getTargetId())
                        .arn(target.getTargetArn())
                        .roleArn(target.getRoleArn())
                        .build();

                PutTargetsRequest request = PutTargetsRequest.builder()
                        .rule(ruleName)
                        .eventBusName(eventBusName)
                        .targets(eventTarget)
                        .build();

                PutTargetsResponse response = eventBridgeClient.putTargets(request);

                TargetResult result = TargetResult.builder()
                        .targetId(target.getTargetId())
                        .targetArn(target.getTargetArn())
                        .ruleName(ruleName)
                        .failed(response.failedEntryCount() > 0)
                        .build();

                log.info("Target added to rule successfully: {}", target.getTargetId());
                return result;

            } catch (Exception e) {
                log.error("Error adding target to rule", e);
                throw new RuntimeException("Failed to add target to rule", e);
            }
        });
    }

    /**
     * List EventBridge rules
     */
    public CompletableFuture<List<EventRuleInfo>> listEventRules() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Listing EventBridge rules");

                ListRulesRequest request = ListRulesRequest.builder()
                        .eventBusName(eventBusName)
                        .limit(50)
                        .build();

                ListRulesResponse response = eventBridgeClient.listRules(request);

                List<EventRuleInfo> rules = response.rules().stream()
                        .map(rule -> EventRuleInfo.builder()
                                .ruleName(rule.name())
                                .ruleArn(rule.arn())
                                .description(rule.description())
                                .state(rule.state().toString())
                                .eventPattern(rule.eventPattern())
                                .build())
                        .toList();

                log.info("Found {} EventBridge rules", rules.size());
                return rules;

            } catch (Exception e) {
                log.error("Error listing EventBridge rules", e);
                throw new RuntimeException("Failed to list EventBridge rules", e);
            }
        });
    }

    /**
     * Send system notification event
     */
    public CompletableFuture<EventResult> sendSystemNotificationEvent(SystemNotificationEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Sending system notification event: {}", event.getNotificationType());

                String eventDetail = String.format("""
                    {
                        "notificationId": "%s",
                        "notificationType": "%s",
                        "recipient": "%s",
                        "message": "%s",
                        "priority": "%s",
                        "scheduledAt": "%s",
                        "channel": "%s",
                        "metadata": %s
                    }
                    """, event.getNotificationId(), event.getNotificationType(),
                         event.getRecipient(), event.getMessage(),
                         event.getPriority(), event.getScheduledAt(),
                         event.getChannel(), event.getMetadata() != null ? 
                         event.getMetadata().toString() : "{}");

                CustomEvent customEvent = CustomEvent.builder()
                        .eventType("System Notification")
                        .eventData(eventDetail)
                        .resources(List.of("notification:" + event.getNotificationId()))
                        .build();

                return sendEvent(customEvent).join();

            } catch (Exception e) {
                log.error("Error sending system notification event", e);
                throw new RuntimeException("Failed to send system notification event", e);
            }
        });
    }

    /**
     * Get EventBridge health status
     */
    public CompletableFuture<Map<String, Object>> getEventBridgeHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test connection by listing rules
                ListRulesRequest request = ListRulesRequest.builder()
                        .eventBusName(eventBusName)
                        .limit(1)
                        .build();
                
                eventBridgeClient.listRules(request);

                return Map.of(
                        "status", "UP",
                        "service", "EventBridge",
                        "eventBus", eventBusName,
                        "timestamp", java.time.Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("EventBridge health check failed", e);
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
    public static class CustomEvent {
        private String eventType;
        private String eventData;
        private List<String> resources;
    }

    @Data
    @lombok.Builder
    public static class StudentEnrollmentEvent {
        private String studentId;
        private String studentName;
        private String courseId;
        private String courseName;
        private String semester;
        private String enrollmentDate;
        private String department;
        private String enrollmentType; // NEW, DROP, WAITLIST
    }

    @Data
    @lombok.Builder
    public static class GradeUpdateEvent {
        private String studentId;
        private String courseId;
        private String assignmentId;
        private double oldGrade;
        private double newGrade;
        private String gradedBy;
        private String gradedAt;
        private String semester;
    }

    @Data
    @lombok.Builder
    public static class SystemNotificationEvent {
        private String notificationId;
        private String notificationType; // EMAIL, SMS, PUSH
        private String recipient;
        private String message;
        private String priority; // HIGH, MEDIUM, LOW
        private String scheduledAt;
        private String channel;
        private Map<String, Object> metadata;
    }

    @Data
    @lombok.Builder
    public static class EventResult {
        private String eventId;
        private String eventStatus;
        private int failedEntryCount;
        private String timestamp;
    }

    @Data
    @lombok.Builder
    public static class EventRuleConfig {
        private String ruleName;
        private String description;
        private String eventPattern;
    }

    @Data
    @lombok.Builder
    public static class RuleTarget {
        private String targetId;
        private String targetArn;
        private String roleArn;
    }

    @Data
    @lombok.Builder
    public static class RuleCreationResult {
        private String ruleArn;
        private String ruleName;
        private String eventBusName;
        private String description;
        private String state;
    }

    @Data
    @lombok.Builder
    public static class TargetResult {
        private String targetId;
        private String targetArn;
        private String ruleName;
        private boolean failed;
    }

    @Data
    @lombok.Builder
    public static class EventRuleInfo {
        private String ruleName;
        private String ruleArn;
        private String description;
        private String state;
        private String eventPattern;
    }
}