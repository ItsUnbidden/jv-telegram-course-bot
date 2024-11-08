package com.unbidden.telegramcoursesbot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ApplicationConfig {
    @Bean
    public ReplyKeyboardRemove keyboardRemove() {
        return ReplyKeyboardRemove.builder()
                .removeKeyboard(true)
                .build();
    }
}
