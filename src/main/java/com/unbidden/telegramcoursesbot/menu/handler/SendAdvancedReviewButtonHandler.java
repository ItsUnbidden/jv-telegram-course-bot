package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendAdvancedReviewButtonHandler extends AbstractButtonHandler {
    private static final String SERVICE_REVIEW_CONTENT_REQUEST = "service_review_content_request";

    private final ContentSessionService sessionService;

    private final ReviewService reviewService;

    private final CourseService courseService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.LEAVE_REVIEW)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        sessionService.createSession(user, bot, m -> {
                reviewService.commitAdvancedReview(reviewService.getReviewByCourseAndUser(user,
                    courseService.getCourseByName(params[0], user, bot)).getId(), user,
                    contentService.parseAndPersistContent(bot, m));
        });
        final Localization request = localizationLoader.localize(
                SERVICE_REVIEW_CONTENT_REQUEST, user);
        clientManager.getClient(bot).sendMessage(user, request);
    }
}
