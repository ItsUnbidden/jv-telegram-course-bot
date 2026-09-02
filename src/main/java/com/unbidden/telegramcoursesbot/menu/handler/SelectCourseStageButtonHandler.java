package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SelectCourseStageButtonHandler extends AbstractButtonHandler {
    private static final String LESSON_ID_PARAM = "terminal";
    private static final String COURSE_ID_PARAM = "courseId";

    private final CourseOrchestrationService courseService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        courseService.selectStage(botRole, Long.parseLong(params.get(COURSE_ID_PARAM)),
                Integer.parseInt(params.get(LESSON_ID_PARAM)));
    }
}
