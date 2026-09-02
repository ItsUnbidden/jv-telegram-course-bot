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
    private final ContentSessionService sessionService;
    
    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final ValidatorUtil validatorUtil;
    
    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        sessionService.createSession(botRole, p -> {
            validatorUtil.checkExactExpectedMessages(p.botRole(), p.messages(), 1);
            final Long contentId = validatorUtil.parseId(p.botRole(), p.messages().getFirst());

            contentService.sendContent(p.botRole(), contentId);
            clientManager.sendMessage(p.botRole(), loader.localize(
                    Localizations.Service.GET_CONTENT_SUCCESS, p.botRole(),
                    new Localizations.Service.GetContentSuccessParams(contentId)));
        }, true);

        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.GET_CONTENT_REQUEST, botRole));
    }
}
