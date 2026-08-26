package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.LogDao;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error.CriticalDirectorNotificationParams;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error.UnspecifiedExceptionParams;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.RuntimeIOException;
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

    private final LogDao logDao;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final ReplyKeyboardRemove keyboardRemove;

    private final EntityUtil entityUtil;

    @Override
    public SendMessage compileSendMessage(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull Exception exc) {
        LOGGER.error("Unspecified exception has occured during user " + user.getId()
                + "'s session.", exc);

        final Localization errorLoc = localizationLoader.localize(
                Error.UNSPECIFIED_EXCEPTION, user, new UnspecifiedExceptionParams(exc.getMessage(),
                exc.getClass().getSimpleName()));

        // notifyDirector(exc, user, bot);

        return SendMessage.builder()
                .chatId(user.getId())
                .text(errorLoc.getData())
                .entities(errorLoc.getEntities())
                .replyMarkup(keyboardRemove)
                .build();
    }

    private void notifyDirector(@NonNull Exception exc, @NonNull UserEntity user,
            @NonNull Bot bot) {
        final UserEntity diretor = entityUtil.getDiretor();
        final InputStream stream = logDao.readCurrentLogFile();

        final Localization criticalErrorDirectorNotification = localizationLoader
                .localize(Error.CRITICAL_DIRECTOR_NOTIFICATION, diretor,
                new CriticalDirectorNotificationParams(exc.getMessage(), exc.getClass().getSimpleName(),
                    user.getId(), bot.getId()));

        clientManager.getBotLordClient().sendMessage(diretor,
                criticalErrorDirectorNotification);
        
        try {
            clientManager.getBotLordClient().execute(SendDocument.builder()
                    .chatId(diretor.getId())
                    .document(new InputFile(stream, CURRENT_LOG_FILE_NAME))
                    .build());
            LOGGER.info("Current log file sent to the Director.");
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to send log file to the Director after "
                    + "an exception occured.", e);
        } finally {
            try {
                stream.close();
                LOGGER.debug("Log file sending stream closed.");
            } catch (IOException e) {
                throw new RuntimeIOException("Unable to close the stream for reading log file.");
            }
        }
    }
}
