package com.notifications.service;

import com.notifications.model.NotificationSqlEntity;
import com.notifications.model.NotificationMongoEntity;

import java.util.List;

public interface NotificationService {
    NotificationSqlEntity saveNotification(NotificationSqlEntity notification);
    NotificationSqlEntity saveNotification(NotificationMongoEntity notification); // <-- must return SQL entity
    List<NotificationSqlEntity> getAllNotifications();
}


