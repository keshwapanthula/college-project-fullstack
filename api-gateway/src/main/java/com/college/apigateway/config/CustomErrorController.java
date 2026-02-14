package com.college.apigateway.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class CustomErrorController {

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> handleError() {
        Map<String, Object> response = new LinkedHashMap<>();
        
        response.put("error", true);
        response.put("status", 404);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("message", "🔍 Endpoint not found");
        response.put("suggestion", "Try one of the available endpoints below");
        
        // Add helpful information
        Map<String, String> available_endpoints = new LinkedHashMap<>();
        available_endpoints.put("🏠 Home", "/");
        available_endpoints.put("📋 Admin API", "/api/admin/");
        available_endpoints.put("🔔 Notifications API", "/api/notifications/");
        available_endpoints.put("📢 Updates API", "/api/updates/");
        available_endpoints.put("💊 Health Check", "/actuator/health");
        available_endpoints.put("ℹ️ Service Info", "/info");
        
        response.put("available_endpoints", available_endpoints);
        
        Map<String, String> quick_links = new LinkedHashMap<>();
        quick_links.put("Main Portal", "http://localhost:3000");
        quick_links.put("Notifications UI", "http://localhost:3001");
        quick_links.put("Updates UI", "http://localhost:4200");
        quick_links.put("Eureka Dashboard", "http://localhost:8761");
        
        response.put("direct_access", quick_links);
        
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
    }
}