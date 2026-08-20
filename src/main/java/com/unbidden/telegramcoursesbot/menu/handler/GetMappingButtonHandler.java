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
    private static final String MAPPING_ID_PARAM = "mappingId";

    private final ContentSessionService sessionService;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final ValidatorUtil validatorUtil;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        sessionService.createSession(user, bot, p -> {
            validatorUtil.checkExactExpectedMessages(p.user(), p.messages(), 1);

            menuService.initiateMenu(p.user(), p.bot(), MenuKey.MAPPING_SETTINGS, MAPPING_ID_PARAM,
                    validatorUtil.parseId(p.user(), p.messages().getFirst()).toString());
        }, true);
        
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.MAPPING_ID_REQUEST, user));
    }
}
