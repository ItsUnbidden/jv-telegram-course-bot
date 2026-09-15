package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemoveMappingLocalizationButtonHandler extends AbstractButtonHandler {
    private static final String MAPPING_ID_PARAM = "mappingId";
    private static final String CONTENT_ID_PARAM = "contentId";

    private final ContentOrchestrationService contentService;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        contentService.removeLocalization(botRole, Long.parseLong(params.get(MAPPING_ID_PARAM)), Long.parseLong(params.get(CONTENT_ID_PARAM)));
    }
}
