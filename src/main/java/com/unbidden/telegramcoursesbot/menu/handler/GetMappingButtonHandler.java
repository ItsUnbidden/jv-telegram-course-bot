package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
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
    public void handle(BotRole botRole, Map<String, String> params) {
        sessionService.createSession(botRole, p -> {
            validatorUtil.checkExactExpectedMessages(p.botRole(), p.messages(), 1);

            menuService.initiateMenu(p.botRole(), MenuKey.MAPPING_SETTINGS, MAPPING_ID_PARAM,
                    validatorUtil.parseId(p.botRole(), p.messages().getFirst()).toString());
        }, true);
        
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.MAPPING_ID_REQUEST, botRole));
    }
}
