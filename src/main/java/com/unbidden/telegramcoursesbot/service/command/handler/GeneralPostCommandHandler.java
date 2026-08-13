package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class GeneralPostCommandHandler implements CommandHandler {
    private static final String COMMAND = "/generalpost";

    private final MenuOrchestrationService menuService;

    @Override
    @Security(authorities = {AuthorityType.POST, AuthorityType.MAINTENANCE}, isBotLordOnly = true)
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        menuService.initiateMenu(user, bot, MenuKey.GENERAL_POST);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.POST, AuthorityType.MAINTENANCE);
    }
}
