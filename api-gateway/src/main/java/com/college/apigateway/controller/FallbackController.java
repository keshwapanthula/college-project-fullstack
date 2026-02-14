package com.college.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/admin")
    public ResponseEntity<Map<String, String>> adminFallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "message", "Admin service is currently unavailable",
                        "status", "SERVICE_UNAVAILABLE",
                        "timestamp", String.valueOf(System.currentTimeMillis())
                ));
    }

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, String>> notificationsFallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "message", "Notifications service is currently unavailable",
                        "status", "SERVICE_UNAVAILABLE", 
                        "timestamp", String.valueOf(System.currentTimeMillis())
                ));
    }

    @GetMapping("/updates")
    public ResponseEntity<Map<String, String>> updatesFallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "message", "Updates service is currently unavailable",
                        "status", "SERVICE_UNAVAILABLE",
                        "timestamp", String.valueOf(System.currentTimeMillis())
                ));
    }
}