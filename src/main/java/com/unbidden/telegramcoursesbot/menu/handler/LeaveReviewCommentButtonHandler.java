package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveReviewCommentButtonHandler extends AbstractButtonHandler {
    private static final String SERVICE_REVIEW_COMMENT_REQUEST = "service_review_comment_request";

    private final ContentSessionService sessionService;

    private final ReviewService reviewService;
    
    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.SEE_REVIEWS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Review review = reviewService.getReviewById(Long.parseLong(params[0]),
                user, bot);

        sessionService.createSession(user, bot, m -> {
            reviewService.leaveComment(user, review,
                    contentService.parseAndPersistContent(bot, m));
        });

        final Localization request = localizationLoader.localize(
                SERVICE_REVIEW_COMMENT_REQUEST, user);
        
        clientManager.getClient(bot).sendMessage(user, request);
    }
}
