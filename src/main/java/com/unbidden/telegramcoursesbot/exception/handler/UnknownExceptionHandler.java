package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.LogDao;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error.CriticalDirectorNotificationParams;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.io.InputStream;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.RuntimeIOException;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${tcp.exception.inform-director}")
    private boolean informDirector;

    @Override
    public SendMessage compileSendMessage(BotRole botRole, Exception exc) {
        LOGGER.error("User " + botRole.getUser().getId() + " triggered an unspecified exception:", exc);

        final Localization errorLoc = localizationLoader.localize(Error.UNSPECIFIED_EXCEPTION, botRole);

        if (informDirector) {
            LOGGER.info("Sending exception information and logs to director...");
            informDirector(botRole, exc);
        }

        return SendMessage.builder()
                .chatId(botRole.getUser().getId())
                .text(errorLoc.getData())
                .entities(errorLoc.getEntities())
                .replyMarkup(keyboardRemove)
                .build();
    }

    private void informDirector(BotRole botRole, Exception exc) {
        final BotRole diretorRole = entityUtil.getDirectorBotRole(entityUtil.getBotLord().getId());
        final Localization criticalErrorDirectorNotification = localizationLoader.localize(Error.CRITICAL_DIRECTOR_NOTIFICATION,
                diretorRole, new CriticalDirectorNotificationParams(exc.getMessage(), exc.getClass().getSimpleName(),
                diretorRole.getUser().getId(), diretorRole.getBot().getId()));

        clientManager.sendMessage(diretorRole, criticalErrorDirectorNotification);
        
        try (final InputStream stream = logDao.readCurrentLogFile()) {
            try {
                clientManager.getBotLordClient().execute(SendDocument.builder()
                        .chatId(diretorRole.getUser().getId())
                        .document(new InputFile(stream, CURRENT_LOG_FILE_NAME))
                        .build());
                LOGGER.info("Current log file sent to the Director.");
            } catch (TelegramApiException e) {
                LOGGER.error("Unable to send log file to the Director after "
                        + "an exception occured.", e);
            }
        } catch (Exception e) {
            throw new RuntimeIOException("Unable to close the stream for reading log file.");
        }
    }
}
