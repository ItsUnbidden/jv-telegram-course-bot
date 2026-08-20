package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeworkFeedbackToggleButtonHandler extends AbstractButtonHandler {
    private static final String HOMEWORK_ID_PARAM = "homeworkId";

    private final HomeworkOrchestrationService homeworkService;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        homeworkService.toggleFeedbackInclusion(user, bot, Long.parseLong(params.get(HOMEWORK_ID_PARAM)));
    }
}
