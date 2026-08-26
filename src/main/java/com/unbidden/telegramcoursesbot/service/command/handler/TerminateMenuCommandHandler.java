package com.unbidden.telegramcoursesbot.service.command.handler;

import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TerminateMenuCommandHandler implements CommandHandler {
    private static final String COMMAND = "/terminatemenu";

    private final MenuOrchestrationService menuService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        sessionService.createSession(user, bot, p -> {
            menuService.terminateMenu(p.user(), p.messages());
        }, true);

        clientManager.getBotLordClient().sendMessage(user, loader.localize(Localizations.Service.MENU_MANUALLY_REMOVED_REQUEST, user));
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.MAINTENANCE);
    }
}
