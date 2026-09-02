package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.exception.LocalizedException;
import com.unbidden.telegramcoursesbot.model.BotRole;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface LocalizedExceptionHandler extends ExceptionHandler {
    SendMessage compileSendMessageFromLocalizedExc(BotRole borRole, LocalizedException exc);

    @Override
    default SendMessage compileSendMessage(BotRole botRole, Exception exc) {
        if (exc instanceof LocalizedException) {
            return compileSendMessageFromLocalizedExc(botRole, (LocalizedException)exc);
        }
        throw new UnsupportedOperationException("Unable to use this method if the "
                + "exception does not extend " + LocalizedException.class.getName());
    }

    Class<? extends Exception> getExceptionClass();
}
