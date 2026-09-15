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
public class SendAdvancedReviewButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final ContentSessionService sessionService;

    private final ReviewOrchestrationService reviewService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.LEAVE_REVIEW)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));

        sessionService.createSession(botRole, p -> {
            reviewService.commitAdvancedReview(p.botRole(), courseId, p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.REVIEW_CONTENT_REQUEST, botRole));
    }
}
