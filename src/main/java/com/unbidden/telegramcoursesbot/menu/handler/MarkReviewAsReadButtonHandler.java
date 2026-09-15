package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.ReviewOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MarkReviewAsReadButtonHandler extends AbstractButtonHandler {
    private static final String REVIEW_ID_PARAM = "reviewId";

    private final ReviewOrchestrationService reviewService;

    @Override
    @Security(authorities = AuthorityType.SEE_REVIEWS)
    public void handle(BotRole botRole, Map<String, String> params) {
        reviewService.markReviewAsRead(botRole, Long.parseLong(params.get(REVIEW_ID_PARAM)));
    }
}
