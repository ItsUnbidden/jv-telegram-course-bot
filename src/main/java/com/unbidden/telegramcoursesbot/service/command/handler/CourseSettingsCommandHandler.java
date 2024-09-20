package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class CourseSettingsCommandHandler implements CommandHandler {
    private final TelegramBot bot;

    private final MenuService menuService;

    @Override
    public void handle(Message message, String[] commandParts) {
        final User user = message.getFrom();

        if (bot.isAdmin(new com.unbidden.telegramcoursesbot.model.User(user))) {
            menuService.initiateMenu("m_crsOpt", user);
        }
    }

    @Override
    public String getCommand() {
        return "/coursesettings";
    }
}
