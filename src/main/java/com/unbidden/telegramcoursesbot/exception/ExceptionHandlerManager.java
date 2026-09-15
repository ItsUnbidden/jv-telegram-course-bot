package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.exception.handler.GeneralLocalizedExceptionHandler;
import com.unbidden.telegramcoursesbot.exception.handler.LocalizedExceptionHandler;
import com.unbidden.telegramcoursesbot.exception.handler.UnknownExceptionHandler;
import com.unbidden.telegramcoursesbot.model.BotRole;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class ExceptionHandlerManager {
    private static final Logger LOGGER = LogManager.getLogger(ExceptionHandlerManager.class);

    private final List<LocalizedExceptionHandler> handlers;

    private final GeneralLocalizedExceptionHandler generalLocalizedHandler;

    private final UnknownExceptionHandler unknownHandler;

    public ExceptionHandlerManager(List<LocalizedExceptionHandler> handlers,
            @Qualifier("generalLocalizedExceptionHandler") GeneralLocalizedExceptionHandler generalLocalizedHandler,
            UnknownExceptionHandler unknownHandler) {
        this.handlers = handlers;
        this.generalLocalizedHandler = generalLocalizedHandler;
        this.unknownHandler = unknownHandler;
    }

    @NonNull
    public SendMessage handleException(BotRole botRole, Exception exc) {
        LOGGER.debug("User " + botRole.getUser().getId() + " has caused " + exc.getClass().getName()
                + " to occur. Searching for exception handler...");

        if (exc instanceof LocalizedException) {
            LOGGER.debug("Exception is localized.");
            for (LocalizedExceptionHandler handler : handlers) {
                if (exc.getClass().equals(handler.getExceptionClass())) {
                    LOGGER.info("Handler " + handler.getClass().getName()
                            + " has been found. Commencing handling...");
                    return handler.compileSendMessage(botRole, exc);
                }
            }
            LOGGER.debug("No specific localized exception handler has been found for localized "
                    + "exception of type " + exc.getClass().getName() + ". Using general handler...");
            return generalLocalizedHandler.compileSendMessage(botRole, exc);
        }
        
        LOGGER.warn("Exception is not localized. Using unknown exception handler "
                + unknownHandler.getClass().getName() + "...");
        return unknownHandler.compileSendMessage(botRole, exc);
    }
}
