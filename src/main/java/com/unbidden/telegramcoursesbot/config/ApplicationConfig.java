package com.unbidden.telegramcoursesbot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.ServerlessWebhook;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ApplicationConfig {
    @Bean
    public TelegramBotsApi telegramBotsApi() {
        try {
            return new TelegramBotsApi(BotSession.class, new ServerlessWebhook());
        } catch (TelegramApiException e) {
            throw new RuntimeException("Unable to initialize telegram api.", e);
        }
    }

    @Bean
    public DefaultBotOptions defaultBotOptions() {
        return new DefaultBotOptions();
    }

    @Bean
    public ReplyKeyboardRemove keyboardRemove() {
        return ReplyKeyboardRemove.builder()
                .removeKeyboard(true)
                .build();
    }
}
