package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class SupportCommandHandler implements CommandHandler {
    private static final String COMMAND = "/support";

    private final MenuService menuService;

    @Override
    @Security(authorities = AuthorityType.ASK_SUPPORT)
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        menuService.initiateMenu(user, bot, MenuKey.SUPPORT_REQUEST);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.ASK_SUPPORT);
    }
}
