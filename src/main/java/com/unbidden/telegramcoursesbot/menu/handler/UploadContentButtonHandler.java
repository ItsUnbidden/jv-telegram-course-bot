package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UploadContentButtonHandler extends AbstractButtonHandler {
    private final LocalizationLoader localizationLoader;

    private final ContentSessionService sessionService;

    private final ContentOrchestrationService contentService;

    private final ClientManager clientManager;
    
    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(BotRole botRole, Map<String, String> params) {
        sessionService.createSession(botRole, p -> {
            final Content content = contentService.parseAndPersistContent(p.botRole(), p.messages());

            final Localization success = localizationLoader.localize(
                    Localizations.Service.UPLOAD_CONTENT_SUCCESS, botRole,
                    new Localizations.Service.UploadContentSuccessParams(content.getId()));
            clientManager.sendMessage(botRole, success);
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.UPLOAD_CONTENT_REQUEST, botRole));
    }
}
