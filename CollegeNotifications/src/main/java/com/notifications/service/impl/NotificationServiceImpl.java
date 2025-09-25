package com.notifications.service.impl;

import com.notifications.model.NotificationMongoEntity;
import com.notifications.model.NotificationSqlEntity;
import com.notifications.repository.NotificationRepository;
import com.notifications.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    public NotificationServiceImpl(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationSqlEntity saveNotification(NotificationSqlEntity notification) {
        notification.setCreatedAt(LocalDateTime.now());
        return repository.save(notification);
    }

    @Override
    public NotificationSqlEntity saveNotification(NotificationMongoEntity mongoNotification) {
        NotificationSqlEntity sqlEntity = new NotificationSqlEntity();
        sqlEntity.setTitle(mongoNotification.getTitle());
        sqlEntity.setMessage(mongoNotification.getMessage());
        sqlEntity.setCreatedAt(LocalDateTime.now());

        return repository.save(sqlEntity);
    }

    @Override
    public List<NotificationSqlEntity> getAllNotifications() {
        return repository.findAll();
    }
}
