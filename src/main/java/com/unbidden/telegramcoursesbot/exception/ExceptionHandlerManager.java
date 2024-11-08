package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.exception.handler.GeneralLocalizedExceptionHandler;
import com.unbidden.telegramcoursesbot.exception.handler.LocalizedExceptionHandler;
import com.unbidden.telegramcoursesbot.exception.handler.UnknownExceptionHandler;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class ExceptionHandlerManager {
    private static final Logger LOGGER = LogManager.getLogger(ExceptionHandlerManager.class);

    @Autowired
    private List<LocalizedExceptionHandler> handlers;

    @Autowired
    @Qualifier("generalLocalizedExceptionHandler")
    private GeneralLocalizedExceptionHandler generalLocalizedHandler;

    @Autowired
    private UnknownExceptionHandler unknownHandler;

    @NonNull
    public SendMessage handleException(@NonNull UserEntity user, @NonNull Exception exc) {
        LOGGER.info("User " + user.getId() + " has caused " + exc.getClass().getName()
                + " to occur. Searching for exception handler...");

        if (exc instanceof LocalizedException) {
            LOGGER.debug("Exception is localized.");
            for (LocalizedExceptionHandler handler : handlers) {
                if (exc.getClass().equals(handler.getExceptionClass())) {
                    LOGGER.info("Handler " + handler.getClass().getName()
                            + " has been found. Commencing handling...");
                    return handler.compileSendMessage(user, exc);
                }
            }
            LOGGER.debug("No specific localized exception handler has been found for localized "
                    + "exception of type " + exc.getClass().getName()
                    + ". Using general handler...");
            return generalLocalizedHandler.compileSendMessage(user, exc);
        }
        
        LOGGER.warn("Exception is not localized. Using unknown exception handler "
                + unknownHandler.getClass().getName() + "...");
        return unknownHandler.compileSendMessage(user, exc);
    }
}
