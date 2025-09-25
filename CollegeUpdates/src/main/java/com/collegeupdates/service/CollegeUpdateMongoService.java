package com.collegeupdates.service;

import com.collegeupdates.Kafka.CollegeUpdateProducer;
import com.collegeupdates.model.CollegeUpdateMongo;
import com.collegeupdates.repository.CollegeUpdateMongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class CollegeUpdateMongoService {
    @Autowired
    private CollegeUpdateProducer producer;
    @Autowired
    private CollegeUpdateMongoRepository collegeUpdateMongoRepository;
    public void addUpdate(String title, String description  ) {
        // Create MongoDB entity
        CollegeUpdateMongo update = new CollegeUpdateMongo();
        update.setTitle(title);
        update.setDescription(description);
        update.setCreatedAt(LocalDateTime.now());
        // Save to MongoDB
        collegeUpdateMongoRepository.save(update);
        // Send to Kafka
        producer.sendUpdate("New update: " + title + " - " + description);
    }
    public List<CollegeUpdateMongo> getAllUpdates() {
        return collegeUpdateMongoRepository.findAll();
    }
}
