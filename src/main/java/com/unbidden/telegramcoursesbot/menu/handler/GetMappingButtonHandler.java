package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetMappingButtonHandler extends AbstractButtonHandler {
    private final ContentSessionService sessionService;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final ValidatorUtil validatorUtil;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        sessionService.createSession(user, bot, (u, b, m) -> {
            validatorUtil.checkExpectedMessages(u, m, 1);

            menuService.initiateMenu(u, b, MenuKey.MAPPING_SETTINGS, validatorUtil.parseId(u, m.get(0)).toString());
        }, true);
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.MAPPING_ID_REQUEST, user));
    }
}
