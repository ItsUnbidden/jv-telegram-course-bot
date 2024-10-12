package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.exception.handler.ExceptionHandler;
import com.unbidden.telegramcoursesbot.exception.handler.GeneralExceptionHandler;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class ExceptionHandlerManager {
    private static final Logger LOGGER = LogManager.getLogger(ExceptionHandlerManager.class);

    @Autowired
    private List<ExceptionHandler> handlers;

    @Autowired
    private GeneralExceptionHandler defaultHandler;

    @NonNull
    public SendMessage handleException(@NonNull UserEntity user, @NonNull Exception exc) {
        LOGGER.info("User " + user.getId() + " has caused " + exc.getClass().getName()
                + " to occur. Searching for exception handler...");

        for (ExceptionHandler handler : handlers) {
            if (exc.getClass().equals(handler.getExceptionClass())) {
                LOGGER.info("Handler " + handler.getClass().getName()
                        + " has been found. Commencing handling...");
                return handler.compileSendMessage(user, exc);
            }
        }
        LOGGER.warn("No exception handler was found for exception " + exc.getClass().getName()
                + ". Using default exception handler "
                + defaultHandler.getClass().getName() + "...");
        return defaultHandler.compileSendMessage(user, exc);
    }
}
