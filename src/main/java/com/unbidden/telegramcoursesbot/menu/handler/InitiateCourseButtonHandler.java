package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InitiateCourseButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "terminal";
    
    private final CourseOrchestrationService courseService;

    @Override
    @Security(authorities = AuthorityType.LAUNCH_COURSE)
    public void handle(BotRole botRole, Map<String, String> params) {
        courseService.initCourse(botRole, Long.parseLong(params.get(COURSE_ID_PARAM)));
    }
}
