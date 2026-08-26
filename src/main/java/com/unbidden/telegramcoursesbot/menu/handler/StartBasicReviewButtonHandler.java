package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.orchestration.ReviewOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StartBasicReviewButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final ReviewOrchestrationService reviewService;

    @Override
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        reviewService.initiateBasicReview(user, bot, Long.parseLong(params.get(COURSE_ID_PARAM)));
    }
}
