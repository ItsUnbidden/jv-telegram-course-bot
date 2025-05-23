package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateAdvancedReviewButtonHandler implements ButtonHandler {
    private static final String SERVICE_REVIEW_CONTENT_REQUEST = "service_review_content_request";

    private final ContentSessionService sessionService;

    private final ReviewService reviewService;

    private final CourseService courseService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        sessionService.createSession(user, m -> {
                reviewService.updateAdvancedReview(reviewService.getReviewByCourseAndUser(user,
                    courseService.getCourseByName(params[0], user)).getId(),
                    contentService.parseAndPersistContent(m));
        });
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_REVIEW_CONTENT_REQUEST, user);
        client.sendMessage(user, request);
    }
}
