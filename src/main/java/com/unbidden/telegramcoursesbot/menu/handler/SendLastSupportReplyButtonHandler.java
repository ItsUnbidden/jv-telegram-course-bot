package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor
public class SendLastSupportReplyButtonHandler extends AbstractButtonHandler {
    private final SupportOrchestrationService supportService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        supportService.sendLastReply(botRole);
    }
}
