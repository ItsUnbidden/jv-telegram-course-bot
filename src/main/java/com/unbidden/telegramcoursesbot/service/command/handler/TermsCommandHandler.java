package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.dao.LocalizationLoader;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class TermsCommandHandler implements CommandHandler {
    private static final Logger LOGGER = LogManager.getLogger(TermsCommandHandler.class);

    private final LocalizationLoader textLoader;

    private final TelegramBot bot;

    @Override
    public void handle(Message message, String[] commandParts) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .text(textLoader.getTextByNameForUser("message_terms",
                    message.getFrom()))
                .build();

        try {
            LOGGER.info("Sending /terms message to user "
                    + message.getFrom().getId() + "...");
            bot.execute(sendMessage);
            LOGGER.info("Message sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send the terms message to user "
                    + message.getFrom().getId(), e);
        }
    }

    @Override
    public String getCommand() {
        return "/terms";
    }
}
