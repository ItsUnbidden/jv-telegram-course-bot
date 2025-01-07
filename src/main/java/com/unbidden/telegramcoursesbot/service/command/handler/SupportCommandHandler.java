package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class SupportCommandHandler implements CommandHandler {
    private static final String COMMAND = "/support";

    private static final String SUPPORT_REQUEST_MENU = "m_sr";

    private final SupportService supportService;

    private final MenuService menuService;

    @Override
    @Security(authorities = AuthorityType.ASK_SUPPORT)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Message message,
            @NonNull String[] commandParts) {
        supportService.checkifUserIsStaffMember(user, bot);

        menuService.initiateMenu(SUPPORT_REQUEST_MENU, user, bot);
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    @NonNull
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.ASK_SUPPORT);
    }
}
