package com.unbidden.telegramcoursesbot.service.menu.handler;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResendSessionButtonHandler implements ButtonHandler {
    private final ContentSessionService contentSessionService;

    @Override
    public void handle(UserEntity user, Bot bot, String[] params) {
        contentSessionService.resend(UUID.fromString(params[0]), user);
    }
}
