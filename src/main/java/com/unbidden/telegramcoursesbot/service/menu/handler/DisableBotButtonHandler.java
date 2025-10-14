package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class DisableBotButtonHandler implements ButtonHandler {
    private final BotService botService;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.BOTS_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);
        clientManager.getBotFatherClient().sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text("This is not implemented at the moment.")
                .build());
    }
}
