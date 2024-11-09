package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetArchiveReviewsButtonHandler implements ButtonHandler {
    private final ReviewService reviewService;

    private final UserService userService;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Long courseId = Long.parseLong(params[0]);

            if (courseId != -1L) {
                reviewService.sendArchiveReviewsForUserAndCourse(user, courseId);
            } else {
                reviewService.sendArchiveReviewsForUser(user);
            }
        }
    }
}
