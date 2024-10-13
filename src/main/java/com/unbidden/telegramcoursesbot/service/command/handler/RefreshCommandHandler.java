package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class RefreshCommandHandler implements CommandHandler {
    private static final String COMMAND = "/refresh";

    private static final String SERVICE_REFRESH_SUCCESS = "service_refresh_success";
    
    private final UserService userService;

    private final LocalizationLoader localizationLoader;
    
    private final TelegramBot bot;

    @Autowired
    @Lazy
    private CommandHandlerManager commandHandlerManager;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        if (userService.isAdmin(message.getFrom())) {
            bot.setOnMaintenance(true);

            localizationLoader.reloadResourses();
            bot.setUpMenuButton();
            localizationLoader.getAvailableLanguageCodes().forEach(c -> bot.setUpUserMenu(c,
                    commandHandlerManager.getUserCommands()));
            userService.getAdminList().forEach(a -> bot.setUpMenuForAdmin(a,
                    commandHandlerManager.getAllCommands()));
            
            bot.setOnMaintenance(false);

            final Localization localization = localizationLoader.getLocalizationForUser(
                    SERVICE_REFRESH_SUCCESS, message.getFrom());
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
        return COMMAND;
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }
}
