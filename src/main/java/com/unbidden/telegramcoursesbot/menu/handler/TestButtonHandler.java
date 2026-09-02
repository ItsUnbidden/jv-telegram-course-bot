package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.BotRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TestButtonHandler extends AbstractButtonHandler {
    private final ClientManager clientManager;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        clientManager.sendMessage(botRole, SendMessage.builder().chatId(botRole.getUser().getId()).text("This is a test button.").build());
    }
}
