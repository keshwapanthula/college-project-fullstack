package com.admin.controller;

import com.admin.model.AdminEntity;
import com.admin.service.AdminService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService service;
    public AdminController(AdminService service) { this.service = service; }

    @PostMapping
    public AdminEntity create(@RequestBody AdminEntity admin) { return service.create(admin); }

    @GetMapping
    public List<AdminEntity> getAll() { return service.getAll(); }
}
