package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateReviewCommentButtonHandler implements ButtonHandler {
    private static final String SERVICE_UPDATE_REVIEW_COMMENT_REQUEST = "service_update_review_comment_request";

    private static final String ERROR_UPDATE_COMMENT_FORBIDDEN = "error_update_comment_forbidden";

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final ReviewService reviewService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Review review = reviewService.getReviewById(Long.parseLong(params[0]));
            
            if (!review.getCommentedBy().getId().equals(user.getId())) {
                throw new ForbiddenOperationException("Review " + review.getId() + "'s comment "
                        + "content cannot be updated by user " + user.getId()
                        + " because they are not the ones who left it", localizationLoader
                        .getLocalizationForUser(ERROR_UPDATE_COMMENT_FORBIDDEN, user));
            }

            sessionService.createSession(user, m -> {
                reviewService.updateComment(user, review,
                        contentService.parseAndPersistContent(m));
            });

            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_UPDATE_REVIEW_COMMENT_REQUEST, user);
            
            client.sendMessage(user, request);      
        }
    }
}
