package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class GeneralPostButtonHandler extends AbstractButtonHandler {
    private final ClientManager clientManager;

    @Override
    @Security(authorities = {AuthorityType.MAINTENANCE, AuthorityType.POST}, isBotLordOnly = true)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        clientManager.getBotLordClient().sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text("General posts are currently disabled.")
                .build());
    }
}
