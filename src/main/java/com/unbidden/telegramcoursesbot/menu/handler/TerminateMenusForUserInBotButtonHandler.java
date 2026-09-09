package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class TerminateMenusForUserInBotButtonHandler extends AbstractButtonHandler {
    private final ContentSessionService sessionService;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(BotRole botRole, Map<String, String> params) {
        sessionService.createSession(botRole, p -> {
            menuService.terminateMenusForUserInBot(p.botRole(), p.messages());
        });

        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.MENUS_MANUALLY_REMOVED_FOR_USER_IN_BOT_REQUEST, botRole));
    }
}
