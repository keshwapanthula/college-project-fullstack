package com.college.admin.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Generic Exception Mapper for Jersey REST endpoints
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Response toResponse(Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("message", exception.getMessage());
        errorResponse.put("type", exception.getClass().getSimpleName());
        
        int status = 500; // Default internal server error
        
        // You can customize status codes based on exception types
        if (exception instanceof IllegalArgumentException) {
            status = 400;
        }
        
        try {
            String json = objectMapper.writeValueAsString(errorResponse);
            return Response.status(status)
                    .entity(json)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (JsonProcessingException e) {
            return Response.status(500)
                    .entity("{\"error\":\"Internal server error\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}