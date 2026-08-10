package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class MaintenanceCommandHandler implements CommandHandler {
    private static final String COMMAND = "/maintenance";

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        clientManager.toggleMaintenance(user);
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
