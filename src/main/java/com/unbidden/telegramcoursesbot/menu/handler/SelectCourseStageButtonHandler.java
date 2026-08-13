package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SelectCourseStageButtonHandler extends AbstractButtonHandler {
    private static final String LESSON_ID_PARAM = "terminal";
    private static final String COURSE_ID_PARAM = "courseId";

    private final CourseOrchestrationService courseService;

    @Override
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        courseService.selectStage(user, bot, Long.parseLong(params.get(COURSE_ID_PARAM)),
                Integer.parseInt(LESSON_ID_PARAM));
    }
}
