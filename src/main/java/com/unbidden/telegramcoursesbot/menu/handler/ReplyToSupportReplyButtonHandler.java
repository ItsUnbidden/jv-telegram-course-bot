package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyToSupportReplyButtonHandler extends AbstractButtonHandler {
    private static final String REPLY_ID_PARAM = "replyId";

    private final ContentSessionService sessionService;
    
    private final SupportOrchestrationService supportService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.REPLY_SUPPORT)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long replyId = Long.parseLong(params.get(REPLY_ID_PARAM));

        supportService.isUserEligibleForSupport(botRole);
        
        sessionService.createSession(botRole, p -> {
            supportService.replyToReply(p.botRole(), replyId, p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.SUPPORT_REPLY_REPLY_REQUEST, botRole));
    }
}
