package com.admin.service;

import com.admin.model.AdminEntity;
import com.admin.repository.AdminRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AdminKafkaListener {

    private final AdminRepository repo;

    public AdminKafkaListener(AdminRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = "college-updates-topic", groupId = "admin-group")
    public void listen(String message) {
        // Save as admin notification/event
        AdminEntity admin = new AdminEntity();
        admin.setName("System");
        admin.setRole(message); // store message in role field for demo
        repo.save(admin);
        System.out.println("Admin received: " + message);
    }
}
