package com.admin.service.cloud;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Hybrid Cloud Messaging Service
 * Provides messaging capabilities using both AWS (SQS/SNS) and Azure (Service Bus)
 * Demonstrates multi-cloud messaging patterns for enterprise microservices
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudMessagingService {

    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final ServiceBusSenderClient serviceBusSenderClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-url:}")
    private String sqsQueueUrl;

    @Value("${aws.sns.topic-arn:}")
    private String snsTopicArn;

    @Value("${cloud.messaging.primary-provider:aws}")
    private String primaryProvider;

    /**
     * Send message to primary messaging provider with automatic failover
     */
    public void sendMessage(Object message, String messageType) {
        try {
            String messageBody = objectMapper.writeValueAsString(message);
            
            if ("aws".equalsIgnoreCase(primaryProvider)) {
                sendToAws(messageBody, messageType);
            } else {
                sendToAzure(messageBody, messageType);
            }
            
            log.info("Message sent successfully via {} provider: {}", primaryProvider, messageType);
            
        } catch (Exception e) {
            log.error("Primary messaging failed, attempting failover: {}", e.getMessage());
            
            try {
                String messageBody = objectMapper.writeValueAsString(message);
                
                if ("aws".equalsIgnoreCase(primaryProvider)) {
                    sendToAzure(messageBody, messageType);
                } else {
                    sendToAws(messageBody, messageType);
                }
                
                log.info("Message sent successfully via failover provider: {}", messageType);
                
            } catch (Exception failoverException) {
                log.error("Both messaging providers failed: {}", failoverException.getMessage());
                throw new RuntimeException("All messaging providers failed", failoverException);
            }
        }
    }

    /**
     * Send message to AWS SQS (Queue) or SNS (Topic)
     */
    private void sendToAws(String messageBody, String messageType) {
        try {
            if ("notification".equalsIgnoreCase(messageType) || "broadcast".equalsIgnoreCase(messageType)) {
                // Use SNS for notifications and broadcasts
                PublishRequest publishRequest = PublishRequest.builder()
                        .topicArn(snsTopicArn)
                        .message(messageBody)
                        .subject("College Admin - " + messageType)
                        .build();
                
                snsClient.publish(publishRequest);
                log.info("Message published to SNS topic: {}", messageType);
                
            } else {
                // Use SQS for regular queued messages
                SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                        .queueUrl(sqsQueueUrl)
                        .messageBody(messageBody)
                        .messageGroupId("college-admin-" + messageType)
                        .messageDeduplicationId(java.util.UUID.randomUUID().toString())
                        .build();
                
                sqsClient.sendMessage(sendMessageRequest);
                log.info("Message sent to SQS queue: {}", messageType);
            }
            
        } catch (Exception e) {
            log.error("AWS messaging failed: {}", e.getMessage());
            throw new RuntimeException("AWS messaging failed", e);
        }
    }

    /**
     * Send message to Azure Service Bus
     */
    private void sendToAzure(String messageBody, String messageType) {
        try {
            ServiceBusMessage serviceBusMessage = new ServiceBusMessage(messageBody)
                    .setSubject("College Admin - " + messageType)
                    .setContentType("application/json")
                    .setMessageId(java.util.UUID.randomUUID().toString());
            
            // Add custom properties
            serviceBusMessage.getApplicationProperties().put("messageType", messageType);
            serviceBusMessage.getApplicationProperties().put("source", "college-admin-service");
            serviceBusMessage.getApplicationProperties().put("timestamp", java.time.Instant.now().toString());
            
            serviceBusSenderClient.sendMessage(serviceBusMessage);
            log.info("Message sent to Azure Service Bus: {}", messageType);
            
        } catch (Exception e) {
            log.error("Azure Service Bus messaging failed: {}", e.getMessage());
            throw new RuntimeException("Azure Service Bus messaging failed", e);
        }
    }

    /**
     * Send notification specifically (prioritizes broadcast channels)
     */
    public void sendNotification(String title, String body, String recipient) {
        NotificationMessage notification = NotificationMessage.builder()
                .title(title)
                .body(body)
                .recipient(recipient)
                .timestamp(java.time.LocalDateTime.now())
                .source("college-admin")
                .build();
        
        sendMessage(notification, "notification");
    }

    /**
     * Send admin alert
     */
    public void sendAdminAlert(String alertLevel, String message, String details) {
        AdminAlert alert = AdminAlert.builder()
                .level(alertLevel)
                .message(message)
                .details(details)
                .timestamp(java.time.LocalDateTime.now())
                .service("college-admin")
                .build();
        
        sendMessage(alert, "admin-alert");
    }

    // Inner classes for message types
    @lombok.Data
    @lombok.Builder
    public static class NotificationMessage {
        private String title;
        private String body;
        private String recipient;
        private java.time.LocalDateTime timestamp;
        private String source;
    }

    @lombok.Data
    @lombok.Builder
    public static class AdminAlert {
        private String level;
        private String message;
        private String details;
        private java.time.LocalDateTime timestamp;
        private String service;
    }
}