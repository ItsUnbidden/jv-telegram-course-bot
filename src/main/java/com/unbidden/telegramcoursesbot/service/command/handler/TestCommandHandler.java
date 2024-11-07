package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class TestCommandHandler implements CommandHandler {
    private static final String TEST_MENU = "m_tst";

    private static final String COMMAND = "/test";

    private final TelegramBot bot;

    private final UserService userService;

    private final MenuService menuService;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final UserEntity userFromDb = userService.getUser(message.getFrom().getId());

        if (userService.isAdmin(userFromDb)) {
            
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
