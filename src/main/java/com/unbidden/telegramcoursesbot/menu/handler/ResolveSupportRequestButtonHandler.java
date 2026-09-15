package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResolveSupportRequestButtonHandler extends AbstractButtonHandler {
    private static final String REQUEST_ID_PARAM = "requestId";
    
    private final SupportOrchestrationService supportService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        supportService.markAsResolved(botRole, Long.parseLong(params.get(REQUEST_ID_PARAM)));
    }
}
