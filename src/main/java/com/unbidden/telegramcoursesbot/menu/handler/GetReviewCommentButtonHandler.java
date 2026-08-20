package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
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
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        contentService.sendContent(user, bot, Long.parseLong(params.get(CONTENT_ID_PARAM)));
    }
}
