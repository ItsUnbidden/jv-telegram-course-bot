package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
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

    private final MenuService menuService;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        if (userService.isAdmin(message.getFrom())) {
            bot.sendMessage(SendMessage.builder()
                    .chatId(message.getFrom().getId())
                    .text("This is a debug command.")
                    .build());
            menuService.initiateMenu("m_tst", message.getFrom());
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/test";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }
}
