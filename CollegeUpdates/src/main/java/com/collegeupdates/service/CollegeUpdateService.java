package com.collegeupdates.service;

import com.collegeupdates.model.CollegeUpdate;

import java.util.List;

public interface CollegeUpdateService {
    CollegeUpdate createUpdate(CollegeUpdate update);
    List<CollegeUpdate> getAllUpdates();
}

