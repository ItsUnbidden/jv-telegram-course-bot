package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.BotRole;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class CourseSettingsCommandHandler implements CommandHandler {
    private static final String COMMAND = "/coursesettings";

    private final MenuOrchestrationService menuService;

    @Override
    @Security(authorities = {AuthorityType.COURSE_SETTINGS, AuthorityType.GIVE_COURSE})
    public void handle(BotRole botRole, Message message, String[] commandParts) {
        menuService.initiateMenu(botRole, MenuKey.COURSE_SETTINGS);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.COURSE_SETTINGS, AuthorityType.GIVE_COURSE);
    }
}
