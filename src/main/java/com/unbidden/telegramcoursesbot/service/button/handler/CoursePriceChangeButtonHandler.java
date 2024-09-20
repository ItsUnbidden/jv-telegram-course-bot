package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class CoursePriceChangeButtonHandler implements ButtonHandler {
    private final TelegramBot bot;

    @Override
    public void handle(String[] params, User user) {
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text("Course price change button was pressed.")
                .build());
    }
}
