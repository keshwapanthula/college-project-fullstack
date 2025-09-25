package com.admin.service;

import com.admin.model.AdminEntity;
import java.util.List;

public interface AdminService {
    AdminEntity create(AdminEntity admin);
    List<AdminEntity> getAll();
}
