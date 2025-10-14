package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateReviewCommentButtonHandler implements ButtonHandler {
    private static final String SERVICE_UPDATE_REVIEW_COMMENT_REQUEST = "service_update_review_comment_request";

    private static final String ERROR_UPDATE_COMMENT_FORBIDDEN = "error_update_comment_forbidden";

    private final ContentSessionService sessionService;

    private final ReviewService reviewService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.SEE_REVIEWS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Review review = reviewService.getReviewById(Long.parseLong(params[0]),
                user, bot);
        
        if (!review.getCommentedBy().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Review " + review.getId() + "'s comment "
                    + "content cannot be updated by user " + user.getId()
                    + " because they are not the ones who left it", localizationLoader
                    .getLocalizationForUser(ERROR_UPDATE_COMMENT_FORBIDDEN, user));
        }

        sessionService.createSession(user, bot, m -> {
            reviewService.updateComment(user, review,
                    contentService.parseAndPersistContent(bot, m));
        });

        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_UPDATE_REVIEW_COMMENT_REQUEST, user);
        
        clientManager.getClient(bot).sendMessage(user, request);      
    }
}
