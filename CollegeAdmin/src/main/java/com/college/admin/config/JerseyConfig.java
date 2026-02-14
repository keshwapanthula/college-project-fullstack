package com.college.admin.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.ws.rs.ApplicationPath;

/**
 * Jersey Configuration for JAX-RS endpoints
 * This provides REST API functionality alongside Spring MVC
 */
@Configuration
@ApplicationPath("/api/v1/jersey")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // Register JAX-RS resource classes
        packages("com.college.admin.resources");
        
        // Enable JSON processing
        property(ServerProperties.JSON_PROCESSING_FEATURE_DISABLE, false);
        
        // Enable bean validation
        property(ServerProperties.BV_FEATURE_DISABLE, false);
        
        // Configure exception mapping
        register(new GenericExceptionMapper());
        
        // Enable tracing for debugging
        property(ServerProperties.TRACING, "ALL");
    }
}