package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.ReviewOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveReviewCommentButtonHandler extends AbstractButtonHandler {
    private static final String REVIEW_ID_PARAM = "reviewId";

    private final ContentSessionService sessionService;

    private final ReviewOrchestrationService reviewService;
    
    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.SEE_REVIEWS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long reviewId = Long.parseLong(params.get(REVIEW_ID_PARAM));

        sessionService.createSession(botRole, p -> {
            reviewService.leaveComment(p.botRole(), reviewId, p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.REVIEW_COMMENT_REQUEST, botRole));
    }
}
