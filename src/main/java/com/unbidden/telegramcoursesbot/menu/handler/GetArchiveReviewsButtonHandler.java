package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.ReviewOrchestrationService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetArchiveReviewsButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final ReviewOrchestrationService reviewService;

    @Override
    @Security(authorities = AuthorityType.SEE_REVIEWS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final String courseIdParam = params.get(COURSE_ID_PARAM);

        if (courseIdParam != null) {
            final Long courseId = Long.parseLong(courseIdParam);

            reviewService.sendArchiveReviewsForUserAndCourse(user, bot, courseId);
        } else {
            reviewService.sendArchiveReviewsForUser(user, bot);
        }
    }
}
