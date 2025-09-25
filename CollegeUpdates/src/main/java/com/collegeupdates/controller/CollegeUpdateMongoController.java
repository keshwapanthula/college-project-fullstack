package com.collegeupdates.controller;

import com.collegeupdates.model.CollegeUpdateMongo;
import com.collegeupdates.service.CollegeUpdateMongoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mongo/updates")
public class CollegeUpdateMongoController {

    @Autowired
    private CollegeUpdateMongoService mongoService;

    // POST method to add new update
    @PostMapping
    public ResponseEntity<String> addUpdate(@RequestBody CollegeUpdateMongo update) {
        // Validation
        if (update.getTitle() == null || update.getTitle().isBlank() ||
                update.getDescription() == null || update.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body("Title and Description cannot be empty");
        }

        try {
            mongoService.addUpdate(update.getTitle(), update.getDescription());
            return ResponseEntity.ok("Update added to MongoDB");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add update: " + e.getMessage());
        }
    }

    // GET method to fetch all updates
    @GetMapping
    public ResponseEntity<List<CollegeUpdateMongo>> getAllUpdates() {
        try {
            List<CollegeUpdateMongo> updates = mongoService.getAllUpdates();
            return ResponseEntity.ok(updates);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of()); // return empty list on error
        }
    }
}
