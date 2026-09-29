package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class TransferSupportRequestButtonHandler extends AbstractButtonHandler {
    private static final String REQUEST_ID_PARAM = "requestId";
    private static final String BOT_ROLE_ID_PARAM = "botRoleId";

    private final SupportOrchestrationService supportService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        supportService.transferRequest(botRole, Long.parseLong(params.get(BOT_ROLE_ID_PARAM)), Long.parseLong(params.get(REQUEST_ID_PARAM)));
    }
}
