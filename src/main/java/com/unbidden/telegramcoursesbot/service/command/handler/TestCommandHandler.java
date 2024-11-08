package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class TestCommandHandler implements CommandHandler {
    private static final String COMMAND = "/test";

    private final UserService userService;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final UserEntity userFromDb = userService.getUser(message.getFrom().getId());

        if (userService.isAdmin(userFromDb)) {
            client.sendMessage(SendMessage.builder()
                    .chatId(userFromDb.getId())
                    .text("This is a test command. It currently does nothing🙃.")
                    .build());
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }
}
