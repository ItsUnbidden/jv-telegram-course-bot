package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetPendingHomeworksButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID = "terminal";

    private final HomeworkOrchestrationService homeworkService;

    @Override
    @Security(authorities = AuthorityType.GIVE_HOMEWORK_FEEDBACK)
    public void handle(BotRole botRole, Map<String, String> params) {
        if (params.get(COURSE_ID) == null) {
            homeworkService.sendPendingHomeworks(botRole);
        } else {
            homeworkService.sendPendingHomeworks(botRole, Long.parseLong(params.get(COURSE_ID)));
        }
    }
}
