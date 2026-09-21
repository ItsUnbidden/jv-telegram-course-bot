package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class TransferHomeworkButtonHandler extends AbstractButtonHandler {
    private static final String BOT_ROLE_ID = "botRoleId";
    private static final String PROGRESS_ID = "progressId";

    private final HomeworkOrchestrationService homeworkService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        homeworkService.transferHomework(botRole, Long.parseLong(params.get(BOT_ROLE_ID)), Long.parseLong(params.get(PROGRESS_ID)));
    }
}
