package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetContentButtonHandler extends AbstractButtonHandler {
    private static final String CONTENT_ID_PARAM = "contentId";

    private final ContentSessionService sessionService;
    
    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final ValidatorUtil validatorUtil;
    
    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long contentId = params.get(CONTENT_ID_PARAM) == null ? null : Long.parseLong(params.get(CONTENT_ID_PARAM));

        if (contentId == null) {
            sessionService.createSession(botRole, p -> {
                validatorUtil.checkExactExpectedMessages(botRole, p.messages(), 1);
                contentService.sendContent(p.botRole(), validatorUtil.parseId(botRole, p.messages().getFirst()));
            }, true);
            clientManager.sendMessage(botRole, loader.localize(Localizations.Service.GET_CONTENT_REQUEST, botRole));
        } else {
            contentService.sendContent(botRole, contentId);
        }
    }
}
