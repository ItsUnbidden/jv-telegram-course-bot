package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class UnsupportedOperationExceptionHandler implements ExceptionHandler {
    private static final Logger LOGGER = LogManager
            .getLogger(UnsupportedOperationExceptionHandler.class);

    private static final String ERROR_UNSUPPORTED_OPERATION_EXCEPTION =
            "error_unsupported_operation_exception";

    private final LocalizationLoader localizationLoader;

    @Override
    public SendMessage compileSendMessage(@NonNull UserEntity user, @NonNull Exception exc) {
        final Localization errorLoc = localizationLoader.getLocalizationForUser(
                ERROR_UNSUPPORTED_OPERATION_EXCEPTION, user);
        
        LOGGER.warn("User " + user.getId() + " tried to utilize unsupported module: "
                + exc.getMessage(), exc);
        return SendMessage.builder()
                .chatId(user.getId())
                .text(errorLoc.getData())
                .entities(errorLoc.getEntities())
                .build();
    }

    @Override
    public Class<? extends Exception> getExceptionClass() {
        return UnsupportedOperationException.class;
    }
}
