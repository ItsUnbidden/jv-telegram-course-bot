package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddMappingLocalizationButtonHandler extends AbstractButtonHandler {
    private static final String MAPPING_ID_PARAM = "mappingId";
    private static final String TEXT_ONLY_PARAM = "isTextOnly";

    private final ContentOrchestrationService contentService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long mappingId = Long.parseLong(params.get(MAPPING_ID_PARAM));
        final boolean isTextOnly = Boolean.parseBoolean(params.get(TEXT_ONLY_PARAM));

        sessionService.createSession(botRole, p -> {
            contentService.addNewLocalization(p.botRole(), mappingId, isTextOnly ? List.of(MediaType.TEXT) : List.of(), p.messages());
        });
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.ADD_NEW_LOCALIZATION_REQUEST, botRole,
                    new Localizations.Service.AddNewLocalizationRequestParams(mappingId)));
    }
}
