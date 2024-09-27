package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class TestCommandHandler implements CommandHandler {
    private final TelegramBot bot;

    private final UserService userService;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        if (userService.isAdmin(message.getFrom())) {
            bot.sendMessage(SendMessage.builder()
                    .text("This is a debug command.")
                    .build());
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/test";
    }
}
