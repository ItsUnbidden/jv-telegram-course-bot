package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class BotSettingsCommandHandler implements CommandHandler {
    private static final String COMMAND = "/botsettings";

    private final MenuService menuService;

    private final EntityUtil entityUtil;

    @Override
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        entityUtil.checkBotLord(user, bot);

        menuService.initiateMenu(user, bot, MenuKey.BOT);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.BOTS_SETTINGS);
    }
}
