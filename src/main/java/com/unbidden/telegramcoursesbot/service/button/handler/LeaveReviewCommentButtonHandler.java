package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
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
public class LeaveReviewCommentButtonHandler implements ButtonHandler {
    private static final String SERVICE_REVIEW_COMMENT_REQUEST = "service_review_comment_request";

    private final ContentSessionService sessionService;

    private final ReviewService reviewService;
    
    private final UserService userService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Review review = reviewService.getReviewById(Long.parseLong(params[0]));

            sessionService.createSession(user, m -> {
                reviewService.leaveComment(user, review,
                        contentService.parseAndPersistContent(m));
            });

            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_REVIEW_COMMENT_REQUEST, user);
            
            client.sendMessage(user, request);      
        }
    }
}
