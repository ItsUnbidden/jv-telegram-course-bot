package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class GeneralBanCommandHandler implements CommandHandler {
    private static final String MENU_NAME = "m_gnrlBn";

    private static final String COMMAND = "/generalban";

    private final BotService botService;
    
    private final MenuService menuService;

    @Override
    @Security(authorities = AuthorityType.GENERAL_BANS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Message message,
            @NonNull String[] commandParts) {
        botService.checkBotFather(bot, user);

        menuService.initiateMenu(MENU_NAME, user, bot);
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    @NonNull
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.MAINTENANCE);
    }
}
