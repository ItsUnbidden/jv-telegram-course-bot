package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TestButtonHandler extends AbstractButtonHandler {
    private final ClientManager clientManager;

    @Override
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        clientManager.getClient(bot).sendMessage(SendMessage.builder().chatId(user.getId()).text("This is a test button.").build());
    }
}
