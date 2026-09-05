package com.unbidden.telegramcoursesbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.unbidden.telegramcoursesbot.config.properties")
public class TelegramCoursesBotApplication {
	public static void main(String[] args) {
		SpringApplication.run(TelegramCoursesBotApplication.class, args);
	}
}
