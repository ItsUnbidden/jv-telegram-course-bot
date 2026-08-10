package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class TestCommandHandler implements CommandHandler {
    private static final String COMMAND = "/test";

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        clientManager.getClient(bot).sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text("This is a test command. It currently does nothing🙃.")
                .build());
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.MAINTENANCE);
    }
}
