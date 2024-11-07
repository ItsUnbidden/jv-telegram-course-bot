package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class CreatorCommandHandler implements CommandHandler {
    private static final String COMMAND = "/creator";
    
    private static final String SERVICE_ABOUT_CREATOR = "service_about_creator";

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_ABOUT_CREATOR, message.getFrom());
        final UserEntity user = userService.getUser(message.getFrom().getId());

        bot.sendMessage(user, localization);
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }
}
