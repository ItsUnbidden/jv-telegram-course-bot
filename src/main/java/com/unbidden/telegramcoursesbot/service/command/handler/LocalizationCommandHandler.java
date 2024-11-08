package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class LocalizationCommandHandler implements CommandHandler {
    private static final String COMMAND = "/localization";

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        if (commandParts.length > 2 && userService.isAdmin(message.getFrom())) {
            final Localization localization = localizationLoader.loadLocalization(commandParts[1],
                    commandParts[2]);
            final UserEntity user = userService.getUser(message.getFrom().getId());

            client.sendMessage(user, localization);
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
