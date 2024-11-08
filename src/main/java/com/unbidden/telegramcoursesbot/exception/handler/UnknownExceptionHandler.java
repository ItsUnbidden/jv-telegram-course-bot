package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.dao.LogDao;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class UnknownExceptionHandler implements ExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger(UnknownExceptionHandler.class);

    private static final String CURRENT_LOG_FILE_NAME = "tcb.log";

    private static final String PARAM_EXC_MESSAGE = "${excMessage}";
    private static final String PARAM_EXC_CLASS_NAME = "${excClassName}";
    
    private static final String ERROR_UNSPECIFIED_EXCEPTION = "error_unspecified_exception";
    private static final String ERROR_CRITICAL_DIRECTOR_NOTIFICATION =
            "error_critical_director_notification";

    private final LogDao logDao;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public SendMessage compileSendMessage(@NonNull UserEntity user, @NonNull Exception exc) {
        LOGGER.error("Unspecified exception has occured during user " + user.getId()
                + "'s session.", exc);

        final Localization errorLoc = localizationLoader.getLocalizationForUser(
                ERROR_UNSPECIFIED_EXCEPTION, user, getParameterMap(exc));

        notifyDirector(exc);

        return SendMessage.builder()
                .chatId(user.getId())
                .text(errorLoc.getData())
                .entities(errorLoc.getEntities())
                .build();
    }

    private void notifyDirector(@NonNull Exception exc) {
        final UserEntity diretor = userService.getDiretor();
        final InputStream stream = logDao.readCurrentLogFile();

        final Localization criticalErrorDirectorNotification = localizationLoader
                .getLocalizationForUser(ERROR_CRITICAL_DIRECTOR_NOTIFICATION, diretor,
                getParameterMap(exc));

        client.sendMessage(diretor, criticalErrorDirectorNotification);
        
        // TODO: make sending log files to director optional and configurable in the bot's interface
        try {
            client.execute(SendDocument.builder()
                    .chatId(diretor.getId())
                    .document(new InputFile(stream, CURRENT_LOG_FILE_NAME))
                    .build());
            LOGGER.info("Current log file sent to the director.");
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to send log file to the Director after "
                    + "an exception occured.", e);
        } finally {
            try {
                stream.close();
                LOGGER.debug("Log file sending stream closed.");
            } catch (IOException e) {
                throw new RuntimeException("Unable to close the stream for reading log file.");
            }
        }
    }

    private Map<String, Object> getParameterMap(Exception exc) {
        final Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(PARAM_EXC_MESSAGE, exc.getMessage());
        parameterMap.put(PARAM_EXC_CLASS_NAME, exc.getClass().getSimpleName());
        return parameterMap;
    }
}
