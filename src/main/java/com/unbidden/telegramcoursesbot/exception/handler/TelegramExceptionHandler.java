package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.exception.TelegramException;
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
public class TelegramExceptionHandler implements ExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger(TelegramExceptionHandler.class);

    private static final String PARAM_EXC_MESSAGE = "${excMessage}";

    private static final String ERROR_ENTITY_NOT_FOUND_EXCEPTION =
            "error_telegram_exception";

    private final LocalizationLoader localizationLoader;

    @Override
    public SendMessage compileSendMessage(@NonNull UserEntity user, @NonNull Exception exc) {
        LOGGER.error("Telegram exception occured.", exc);
        
        final Localization errorLoc = localizationLoader.getLocalizationForUser(
                ERROR_ENTITY_NOT_FOUND_EXCEPTION, user, PARAM_EXC_MESSAGE, exc.getMessage());
        
        return SendMessage.builder()
                .chatId(user.getId())
                .text(errorLoc.getData())
                .entities(errorLoc.getEntities())
                .build();
    }

    @Override
    public Class<? extends Exception> getExceptionClass() {
        return TelegramException.class;
    }
}
