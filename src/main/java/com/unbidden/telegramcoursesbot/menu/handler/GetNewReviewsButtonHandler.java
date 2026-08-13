package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetNewReviewsButtonHandler extends AbstractButtonHandler {
    private final ReviewService reviewService;

    @Override
    @Security(authorities = AuthorityType.SEE_REVIEWS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Long courseId = Long.parseLong(params[0]);
        
        if (courseId != -1L) {
            reviewService.sendNewReviewsForUserAndCourse(user, courseId, bot);
        } else {
            reviewService.sendNewReviewsForUser(user, bot);
        }
    }
}
