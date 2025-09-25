package com.notifications;

import com.notifications.service.NotificationService;
import com.notifications.repository.NotificationRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SpringBootTest
class CollegenotificationApplicationTests {

	@Configuration
	static class TestConfig {
		@Bean
		NotificationService notificationService() {
			return Mockito.mock(NotificationService.class);
		}
		@Bean
		NotificationRepository notificationRepository() {
			return Mockito.mock(NotificationRepository.class);
		}
		@Bean
		KafkaTemplate<String, String> kafkaTemplate() {
			return Mockito.mock(KafkaTemplate.class);
		}
	}

	@Test
	void contextLoads() {
		// Passes with minimal code
	}
}


