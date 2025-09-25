package com.notifications.repository;

import com.notifications.model.NotificationMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationMongoRepository extends MongoRepository<NotificationMongoEntity, String> {
}
