package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommitSessionButtonHandler extends AbstractButtonHandler {
    private static final String SESSION_ID_PARAM = "sessionId";

    private final ContentSessionService contentSessionService;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        contentSessionService.commit(botRole, UUID.fromString(params.get(SESSION_ID_PARAM)));
    }
}
