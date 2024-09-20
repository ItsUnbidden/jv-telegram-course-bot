package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class AdminCommandHandler implements CommandHandler {
    private final TelegramBot bot;

    @Override
    public void handle(Message message, String[] commandParts) {
        if (bot.isAdmin(new User(message.getFrom()))) {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(message.getFrom().getId())
                    .text("/admin command is currently not implemented.")
                    .build();
            bot.sendMessage(sendMessage);
        }
    }

    @Override
    public String getCommand() {
        return "/admin";
    }
}
