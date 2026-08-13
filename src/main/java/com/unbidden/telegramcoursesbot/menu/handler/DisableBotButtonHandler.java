package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class DisableBotButtonHandler extends AbstractButtonHandler {
    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.BOTS_SETTINGS, isBotLordOnly = true)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        clientManager.getBotLordClient().sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text("This is not implemented at the moment.")
                .build());
    }
}
