package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
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
public class SendSupportRequestButtonHandler extends AbstractButtonHandler {    
    private final ContentSessionService sessionService;
    
    private final SupportOrchestrationService supportService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.ASK_SUPPORT)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        if (!supportService.isUserEligibleForSupport(user, bot)) {
            throw new ForbiddenOperationException("User " + user.getId() + " cannot send another "
                    + "support request without resolving previous one.", localizationLoader
                    .localize(Localizations.Error.USER_NOT_ELIGIBLE_FOR_SUPPORT, user));
        }
        sessionService.createSession(user, bot, m -> {
            supportService.createNewSupportRequest(user, bot, m.messages(), "");    
        });
        
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.SUPPORT_REQUEST_CONTENT_REQUEST, user));
    }
}
