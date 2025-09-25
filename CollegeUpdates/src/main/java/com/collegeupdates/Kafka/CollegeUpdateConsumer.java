package com.collegeupdates.Kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CollegeUpdateConsumer {

    @KafkaListener(topics = "college-updates", groupId = "college-updates-group")
    public void consume(String message) {
        System.out.println("Received from Kafka: " + message);
    }
}
