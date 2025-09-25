package com.notifications.service;

import com.notifications.model.NotificationSqlEntity;
import com.notifications.repository.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationKafkaListener {

    private final NotificationRepository repo;

    public NotificationKafkaListener(NotificationRepository repo) {
        this.repo = repo;
    }

    @Transactional
    @KafkaListener(topics = "college-updates-topic", groupId = "notification-group")
    public void listen(String message) {
        NotificationSqlEntity notification = new NotificationSqlEntity();
        notification.setTitle("New Update");
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        repo.save(notification);
        System.out.println("Notification received: " + message);
    }
}
