package com.notifications.controller;

import com.notifications.model.NotificationMongoEntity;
import com.notifications.model.NotificationSqlEntity;
import com.notifications.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/notifications") // Base URL
public class NotificationsController {

    private final NotificationService service;

    // Constructor injection (preferred
    public NotificationsController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NotificationSqlEntity> create(@RequestBody NotificationSqlEntity notification) {
        NotificationSqlEntity saved = service.saveNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/mongo")
    public ResponseEntity<NotificationSqlEntity> createFromMongo(@RequestBody NotificationMongoEntity notification) {
        NotificationSqlEntity saved = service.saveNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<NotificationSqlEntity>> getAll() {
        return ResponseEntity.ok(service.getAllNotifications());
    }

}
