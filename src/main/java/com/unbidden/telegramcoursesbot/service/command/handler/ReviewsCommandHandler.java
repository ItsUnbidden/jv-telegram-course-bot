package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class ReviewsCommandHandler implements CommandHandler {
    private static final String COMMAND = "/reviews";

    private final MenuOrchestrationService menuService;

    @Override
    @Security(authorities = {AuthorityType.SEE_REVIEWS})
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        menuService.initiateMenu(user, bot, MenuKey.GET_REVIEWS);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.SEE_REVIEWS);
    }
}
