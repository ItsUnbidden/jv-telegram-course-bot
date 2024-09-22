package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.util.Blockable;
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
    @Blockable
    public void handle(Message message, String[] commandParts) {
        final User user = message.getFrom();

        if (bot.isAdmin(new UserEntity(user))) {
            menuService.initiateMenu("m_crsOpt", user);
        }
    }

    @Override
    public String getCommand() {
        return "/coursesettings";
    }
}
