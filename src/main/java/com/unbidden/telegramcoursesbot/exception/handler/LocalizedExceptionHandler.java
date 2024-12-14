package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.exception.LocalizedException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface LocalizedExceptionHandler extends ExceptionHandler {
    SendMessage compileSendMessageFromLocalizedExc(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull LocalizedException exc);

    @Override
    default SendMessage compileSendMessage(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull Exception exc) {
        if (exc instanceof LocalizedException) {
            return compileSendMessageFromLocalizedExc(user, bot, (LocalizedException)exc);
        }
        throw new UnsupportedOperationException("Unable to use this method if the "
                + "exception does not extend " + LocalizedException.class.getName());
    }

    Class<? extends Exception> getExceptionClass();
}
