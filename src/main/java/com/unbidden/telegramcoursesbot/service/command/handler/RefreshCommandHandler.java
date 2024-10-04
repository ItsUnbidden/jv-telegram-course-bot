package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class RefreshCommandHandler implements CommandHandler {
    private final TelegramBot bot;

    private final LocalizationLoader localizationLoader;

    private final UserService userService;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        if (userService.isAdmin(message.getFrom())) {
            bot.setOnMaintenance(true);

            localizationLoader.reloadResourses();
            bot.setUpMenuButton();
            bot.setUpMenus();
            
            bot.setOnMaintenance(false);

            final Localization localization = localizationLoader.getLocalizationForUser(
                    "service_refresh_success", message.getFrom());
            bot.sendMessage(SendMessage.builder()
                    .chatId(message.getFrom().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/refresh";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }
}
