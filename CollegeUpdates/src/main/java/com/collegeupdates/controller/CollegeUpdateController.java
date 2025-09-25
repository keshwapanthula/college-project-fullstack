package com.collegeupdates.controller;

import com.collegeupdates.model.CollegeUpdate;
import com.collegeupdates.service.CollegeUpdateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collegeupdates")
public class CollegeUpdateController {

    private final CollegeUpdateService service;

    public CollegeUpdateController(CollegeUpdateService service) {
        this.service = service;
    }

    @PostMapping
    public CollegeUpdate addUpdate(@RequestBody CollegeUpdate update) {
        return service.createUpdate(update);  // logic unchanged
    }

    @GetMapping
    public List<CollegeUpdate> getAllUpdates() {
        return service.getAllUpdates();  // logic unchanged
    }
}
