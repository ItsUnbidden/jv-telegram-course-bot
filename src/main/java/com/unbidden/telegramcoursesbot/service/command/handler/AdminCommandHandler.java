package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class AdminCommandHandler implements CommandHandler {
    private final UserService userService;

    private final MenuService menuService;

    @Override
    @Blockable
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final User user = message.getFrom();

        if (userService.isAdmin(user)) {
            menuService.initiateMenu("m_admAct", user);
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/admin";
    }
}
