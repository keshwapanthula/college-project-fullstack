package com.notifications.repository;

import com.notifications.model.NotificationSqlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationSqlEntity, Long> {
}


