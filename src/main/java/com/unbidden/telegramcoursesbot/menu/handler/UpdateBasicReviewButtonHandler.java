package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.orchestration.ReviewOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UpdateBasicReviewButtonHandler extends AbstractButtonHandler {
    private static final String REVIEW_ID_PARAM = "reviewId";
    private static final String NEW_GRADE_PARAM = "terminal";

    private final ReviewOrchestrationService reviewService;
    
    @Override
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        reviewService.updateCourseGrade(user, bot, Long.parseLong(params.get(REVIEW_ID_PARAM)),
                Integer.parseInt(params.get(NEW_GRADE_PARAM)));
    }
}
