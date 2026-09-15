package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.ReviewOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SendBasicReviewButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_GRADE_PARAM = "terminal";
    private static final String COURSE_ID_PARAM = "courseId";

    private final ReviewOrchestrationService reviewService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        reviewService.commitBasicReview(botRole, Long.parseLong(params.get(COURSE_ID_PARAM)), Integer.parseInt(params.get(COURSE_GRADE_PARAM)));
    }
}
