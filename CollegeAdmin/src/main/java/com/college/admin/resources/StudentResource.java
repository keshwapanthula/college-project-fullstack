package com.college.admin.resources;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Jersey JAX-RS Resource for Student Management
 * Demonstrates RESTful web services using Jersey Framework
 */
@Component
@Path("/students")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StudentResource {

    @GET
    public Response getAllStudents() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Students retrieved successfully using Jersey JAX-RS");
        response.put("students", "List of students would be here");
        response.put("framework", "Jersey JAX-RS");
        
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getStudentById(@PathParam("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Student retrieved by ID: " + id);
        response.put("framework", "Jersey JAX-RS");
        response.put("id", id);
        
        return Response.ok(response).build();
    }

    @POST
    public Response createStudent(Map<String, Object> studentData) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Student created successfully");
        response.put("framework", "Jersey JAX-RS");
        response.put("data", studentData);
        
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateStudent(@PathParam("id") Long id, Map<String, Object> studentData) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Student updated successfully");
        response.put("framework", "Jersey JAX-RS");
        response.put("id", id);
        response.put("data", studentData);
        
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteStudent(@PathParam("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Student deleted successfully");
        response.put("framework", "Jersey JAX-RS");
        response.put("id", id);
        
        return Response.ok(response).build();
    }
}