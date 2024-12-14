package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetNewReviewsButtonHandler implements ButtonHandler {
    private final ReviewService reviewService;

    @Override
    @Security(authorities = AuthorityType.SEE_REVIEWS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Long courseId = Long.parseLong(params[0]);
        
        if (courseId != -1L) {
            reviewService.sendNewReviewsForUserAndCourse(user, courseId, bot);
        } else {
            reviewService.sendNewReviewsForUser(user, bot);
        }
    }
}
