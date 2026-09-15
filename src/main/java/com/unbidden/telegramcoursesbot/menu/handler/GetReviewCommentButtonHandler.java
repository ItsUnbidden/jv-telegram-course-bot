package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetReviewCommentButtonHandler extends AbstractButtonHandler {
    private static final String CONTENT_ID_PARAM = "terminal";

    private final ContentOrchestrationService contentService;

    @Override
    @Security(authorities = AuthorityType.SEE_REVIEWS)
    public void handle(BotRole botRole, Map<String, String> params) {
        contentService.sendContent(botRole, Long.parseLong(params.get(CONTENT_ID_PARAM)));
    }
}
