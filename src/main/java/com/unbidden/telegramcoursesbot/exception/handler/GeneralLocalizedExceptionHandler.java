package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.exception.LocalizedException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.BotRole;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

@Component
@RequiredArgsConstructor
public class GeneralLocalizedExceptionHandler implements LocalizedExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger(GeneralLocalizedExceptionHandler.class);

    protected final LocalizationLoader localizationLoader;

    protected final ReplyKeyboardRemove keyboardRemove;

    @Override
    public SendMessage compileSendMessageFromLocalizedExc(BotRole botRole, LocalizedException exc) {
        LOGGER.debug("User " + botRole.getUser().getId() + " has triggered an exception in bot "
                + botRole.getBot().getId() + ": ", exc);

        if (exc.getErrorLocalization() == null) {
            final Localization errorLocalization = localizationLoader.localize(Error.NO_EXCEPTION_LOCALIZATION_AVAILABLE, botRole);

            LOGGER.error("There is no localization available for error message. Default one will be used. This is likely a bug.");
            return SendMessage.builder()
                    .chatId(botRole.getUser().getId())
                    .text(errorLocalization.getData())
                    .entities(errorLocalization.getEntities())
                    .replyMarkup(keyboardRemove)
                    .build();
        }
        LOGGER.debug("Compiling error message to user " + botRole.getUser().getId() + "...");
        return SendMessage.builder()
                .chatId(botRole.getUser().getId())
                .text(exc.getErrorLocalization().getData())
                .entities(exc.getErrorLocalization().getEntities())
                .replyMarkup(keyboardRemove)
                .build();
    }

    @Override
    public Class<? extends Exception> getExceptionClass() {
        return LocalizedException.class;
    }
}
