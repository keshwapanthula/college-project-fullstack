package com.college.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
public class ApiGatewayController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "College Management System API Gateway");
        response.put("status", "✅ Running");
        response.put("version", "1.0.0");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("🏠 Main Portal", "http://localhost:3000");
        endpoints.put("🔔 Notifications", "http://localhost:3001");  
        endpoints.put("📢 Updates", "http://localhost:4200");
        endpoints.put("📊 Eureka Dashboard", "http://localhost:8761");
        endpoints.put("⚙️ Config Server", "http://localhost:8888");
        
        response.put("frontend_applications", endpoints);
        
        Map<String, String> api_endpoints = new LinkedHashMap<>();
        api_endpoints.put("📋 Admin API", "/api/admin/**");
        api_endpoints.put("🔔 Notifications API", "/api/notifications/**");
        api_endpoints.put("📢 Updates API", "/api/updates/**");
        api_endpoints.put("💊 Health Check", "/actuator/health");
        api_endpoints.put("📈 Metrics", "/actuator/metrics");
        
        response.put("api_endpoints", api_endpoints);
        
        Map<String, String> quick_access = new LinkedHashMap<>();
        quick_access.put("Portal UI", "/portal/");
        quick_access.put("Notifications UI", "/notifications-ui/");
        quick_access.put("Updates UI", "/updates-ui/");
        
        response.put("quick_access", quick_access);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health") 
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "API Gateway");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("app", "College Management System");
        response.put("component", "API Gateway");
        response.put("version", "1.0.0");
        response.put("environment", "Development");
        response.put("port", 8080);
        
        Map<String, Object> services = new LinkedHashMap<>();
        services.put("college-admin", "College Administration Backend");
        services.put("college-notifications", "Notifications Service");
        services.put("college-updates", "Updates Service");
        services.put("eureka-server", "Service Registry");
        services.put("config-server", "Configuration Server");
        
        response.put("microservices", services);
        
        return ResponseEntity.ok(response);
    }
}