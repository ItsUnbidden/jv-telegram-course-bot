package com.unbidden.telegramcoursesbot.service.command.handler;

import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TerminateMenuCommandHandler implements CommandHandler {
    private static final String COMMAND = "/menu";

    private final MenuOrchestrationService menuService;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(BotRole botRole, Message message, String[] commandParts) {
        menuService.initiateMenu(botRole, MenuKey.MENU_DEBUG);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.MAINTENANCE);
    }
}
