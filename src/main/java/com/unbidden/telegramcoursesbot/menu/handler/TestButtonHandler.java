package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TestButtonHandler extends AbstractButtonHandler {
    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Localization loc = loader.localize(Localizations.Service.TEST_LOC, botRole);

        clientManager.sendMessage(botRole, loc);
    }
}
