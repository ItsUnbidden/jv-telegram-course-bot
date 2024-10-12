package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class EntityNotFoundExceptionHandler implements ExceptionHandler {
    private static final String ERROR_ENTITY_NOT_FOUND_EXCEPTION =
            "error_entity_not_found_exception";

    private final LocalizationLoader localizationLoader;

    @Override
    public SendMessage compileSendMessage(@NonNull UserEntity user, @NonNull Exception exc) {
        final Localization errorLoc = localizationLoader.getLocalizationForUser(
                ERROR_ENTITY_NOT_FOUND_EXCEPTION, user);
        
        return SendMessage.builder()
                .chatId(user.getId())
                .text(errorLoc.getData())
                .entities(errorLoc.getEntities())
                .build();
    }

    @Override
    public Class<? extends Exception> getExceptionClass() {
        return EntityNotFoundException.class;
    }
}
