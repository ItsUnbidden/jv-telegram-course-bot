package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseMaintenanceToggleButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";
    private static final String SNAPSHOT_ID_PARAM = "snapshotId";

    private final CourseOrchestrationService courseService;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        courseService.toggleMaintenance(botRole, Long.parseLong(params.get(COURSE_ID_PARAM)), Long.parseLong(params.get(SNAPSHOT_ID_PARAM)));
    }
}
