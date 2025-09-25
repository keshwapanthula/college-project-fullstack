package com.admin.service.impl;

import com.admin.model.AdminEntity;
import com.admin.repository.AdminRepository;
import com.admin.service.AdminService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final AdminRepository repo;
    public AdminServiceImpl(AdminRepository repo) { this.repo = repo; }
    public AdminEntity create(AdminEntity admin) { return repo.save(admin); }
    public List<AdminEntity> getAll() { return repo.findAll(); }
}
