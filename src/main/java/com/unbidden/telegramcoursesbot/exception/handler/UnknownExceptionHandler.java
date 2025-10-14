package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.LogDao;
import com.unbidden.telegramcoursesbot.model.Bot;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class UnknownExceptionHandler implements ExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger(UnknownExceptionHandler.class);

    private static final String CURRENT_LOG_FILE_NAME = "tcb.log";

    private static final String PARAM_EXC_MESSAGE = "${excMessage}";
    private static final String PARAM_EXC_CLASS_NAME = "${excClassName}";
    private static final String PARAM_USER_FULL_NAME = "${userFullName}";
    private static final String PARAM_BOT_NAME = "${botName}";
    
    private static final String ERROR_UNSPECIFIED_EXCEPTION = "error_unspecified_exception";
    private static final String ERROR_CRITICAL_DIRECTOR_NOTIFICATION =
            "error_critical_director_notification";

    private final LogDao logDao;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final ReplyKeyboardRemove keyboardRemove;

    @Override
    public SendMessage compileSendMessage(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull Exception exc) {
        LOGGER.error("Unspecified exception has occured during user " + user.getId()
                + "'s session.", exc);

        final Localization errorLoc = localizationLoader.getLocalizationForUser(
                ERROR_UNSPECIFIED_EXCEPTION, user, getParameterMap(exc, user, bot));

        notifyDirector(exc, user, bot);

        return SendMessage.builder()
                .chatId(user.getId())
                .text(errorLoc.getData())
                .entities(errorLoc.getEntities())
                .replyMarkup(keyboardRemove)
                .build();
    }

    private void notifyDirector(@NonNull Exception exc, @NonNull UserEntity user,
            @NonNull Bot bot) {
        final UserEntity diretor = userService.getDiretor();
        final InputStream stream = logDao.readCurrentLogFile();

        final Localization criticalErrorDirectorNotification = localizationLoader
                .getLocalizationForUser(ERROR_CRITICAL_DIRECTOR_NOTIFICATION, diretor,
                getParameterMap(exc, user, bot));

        clientManager.getBotFatherClient().sendMessage(diretor,
                criticalErrorDirectorNotification);
        
        try {
            clientManager.getBotFatherClient().execute(SendDocument.builder()
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

    private Map<String, Object> getParameterMap(Exception exc, UserEntity user, Bot bot) {
        final Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(PARAM_EXC_MESSAGE, exc.getMessage());
        parameterMap.put(PARAM_EXC_CLASS_NAME, exc.getClass().getSimpleName());
        parameterMap.put(PARAM_BOT_NAME, bot.getName());
        parameterMap.put(PARAM_USER_FULL_NAME, user.getFullName());
        return parameterMap;
    }
}
