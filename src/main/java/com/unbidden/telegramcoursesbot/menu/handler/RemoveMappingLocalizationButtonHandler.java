package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemoveMappingLocalizationButtonHandler extends AbstractButtonHandler {
    private static final String MAPPING_ID_PARAM = "mappingId";

    private final ContentOrchestrationService contentService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final ContentMapping mapping = entityUtil.getMappingById(botRole, Long.parseLong(params.get(MAPPING_ID_PARAM)));
        
        sessionService.createSession(botRole, p -> {
            contentService.removeLocalization(p.botRole(), mapping.getId(), p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.REMOVE_LOCALIZATION_FROM_MAPPING_REQUEST, botRole,
                    new Localizations.Service.RemoveLocalizationFromMappingRequestParams(mapping.getId(), mapping.getContent().stream()
                        .map(c -> c.getLanguageCode())
                        .collect(Collectors.joining(", ")))));
    }
}
