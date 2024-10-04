package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class ContentCommandHandler implements CommandHandler {
    private final MenuService menuService;

    private final UserService userService;
    
    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        if (userService.isAdmin(message.getFrom())) {
            menuService.initiateMenu("m_cntAct", message.getFrom());
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/content";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }
}
