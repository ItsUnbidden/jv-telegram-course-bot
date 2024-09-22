package com.unbidden.telegramcoursesbot.service.command.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
public class RefreshCommandHandler implements CommandHandler {

    @Override
    public void handle(Message message, String[] commandParts) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }

    @Override
    public String getCommand() {
        return "/refresh";
    }
}
