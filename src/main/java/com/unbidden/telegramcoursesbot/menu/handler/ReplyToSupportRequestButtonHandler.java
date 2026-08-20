package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyToSupportRequestButtonHandler extends AbstractButtonHandler {
    private static final String REQUEST_ID_PARAM = "requestId";

    private final ContentSessionService sessionService;
    
    private final SupportOrchestrationService supportService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.ANSWER_SUPPORT)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Long requestId = Long.parseLong(params.get(REQUEST_ID_PARAM));

        supportService.isUserEligibleForSupport(user, bot);

        sessionService.createSession(user, bot, p -> {
            supportService.replyToSupportRequest(p.user(), p.bot(), requestId, p.messages());          
        });

        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.SUPPORT_REQUEST_REPLY_REQUEST, user));
    }
}
