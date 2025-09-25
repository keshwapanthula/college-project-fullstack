package com.collegeupdates.service.impl;

import com.collegeupdates.model.CollegeUpdate;
import com.collegeupdates.repository.CollegeUpdatesRepository;
import com.collegeupdates.service.CollegeUpdateService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.kafka.support.KafkaHeaders.TOPIC;

@Service
public class CollegeUpdateServiceImpl implements CollegeUpdateService {

    private final CollegeUpdatesRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CollegeUpdateServiceImpl(CollegeUpdatesRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CollegeUpdate createUpdate(CollegeUpdate update) {
        update.setCreatedAt(LocalDateTime.now());
        CollegeUpdate saved = repository.save(update);

        String message = saved.getTitle() + ": " + saved.getDescription();
        kafkaTemplate.send(TOPIC, message);
        System.out.println("Sent to Kafka: " + message);

        return saved;
    }


    @Override
    public List<CollegeUpdate> getAllUpdates() {
        return repository.findAll();  // logic unchanged
    }
}
