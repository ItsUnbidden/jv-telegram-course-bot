package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommitSessionButtonHandler extends AbstractButtonHandler {
    private static final String SESSION_ID_PARAM = "sessionId";

    private final ContentSessionService contentSessionService;

    @Override
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        contentSessionService.commit(user, bot, UUID.fromString(params.get(SESSION_ID_PARAM)));
    }
}
