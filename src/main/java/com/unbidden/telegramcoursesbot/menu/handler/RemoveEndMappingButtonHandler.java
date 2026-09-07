package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class RemoveEndMappingButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final CourseOrchestrationService courseService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        courseService.removeEndMapping(botRole, Long.parseLong(params.get(COURSE_ID_PARAM)));
    }
}
