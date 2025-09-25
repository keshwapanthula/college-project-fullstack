package com.collegeupdates.repository;

import com.collegeupdates.model.CollegeUpdateMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CollegeUpdateMongoRepository extends MongoRepository<CollegeUpdateMongo, String> {
}
