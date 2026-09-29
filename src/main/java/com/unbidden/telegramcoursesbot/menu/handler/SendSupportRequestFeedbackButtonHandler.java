package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class SendSupportRequestFeedbackButtonHandler extends AbstractButtonHandler {
    private static final String REQUEST_ID_PARAM = "terminal";

    private final SupportOrchestrationService supportService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        supportService.sendSupportRequestFeedback(botRole, Long.parseLong(params.get(REQUEST_ID_PARAM)));
    }
}
