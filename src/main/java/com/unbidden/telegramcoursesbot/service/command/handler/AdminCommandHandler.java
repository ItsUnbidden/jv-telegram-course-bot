package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class AdminCommandHandler implements CommandHandler {
    private static final String ADMIN_MENU = "m_admAct";
    private static final String COMMAND = "/admin";

    private final MenuService menuService;

    @Override
    @Security(authorities = {AuthorityType.BOT_USER_BANS, AuthorityType.ROLE_SETTINGS})
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Message message,
            @NonNull String[] commandParts) {
        menuService.initiateMenu(ADMIN_MENU, user, bot);
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    @NonNull
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.BOT_USER_BANS, AuthorityType.ROLE_SETTINGS);
    }
}
