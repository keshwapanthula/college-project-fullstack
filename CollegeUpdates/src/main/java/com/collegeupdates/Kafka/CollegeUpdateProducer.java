package com.collegeupdates.Kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CollegeUpdateProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CollegeUpdateProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUpdate(String message) {
        kafkaTemplate.send("college-updates-topic", message);
    }
}
