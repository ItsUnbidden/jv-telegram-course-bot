package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class AdminCommandHandler implements CommandHandler {
    private static final String ADMIN_MENU = "m_admAct";
    private static final String COMMAND = "/admin";

    private final UserService userService;

    private final MenuService menuService;

    @Override
    @Blockable
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final User user = message.getFrom();

        if (userService.isAdmin(user)) {
            menuService.initiateMenu(ADMIN_MENU, user);
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
