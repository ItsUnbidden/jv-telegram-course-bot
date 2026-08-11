package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.exception.LocalizedException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error.TelegramInternalParams;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

@Component
public class TelegramExceptionHandler extends GeneralLocalizedExceptionHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(TelegramExceptionHandler.class);

    public TelegramExceptionHandler(LocalizationLoader localizationLoader,
            ReplyKeyboardRemove keyboardRemove) {
        super(localizationLoader, keyboardRemove);
    }

    @Override
    public SendMessage compileSendMessageFromLocalizedExc(@NonNull UserEntity user,
            @NonNull Bot bot, @NonNull LocalizedException exc) {
        LOGGER.error("During user " + user.getId()
                + "'s session a telegram exception occured: ", exc);

        if (exc.getErrorLocalization() == null) {
            final Localization errorLocalization = localizationLoader.localize(
                    Error.TELEGRAM_INTERNAL, user, new TelegramInternalParams(exc.getMessage()));
            LOGGER.debug("There is no localization available for telegram error message. "
                    + "Default one will be used.");
            return SendMessage.builder()
                    .chatId(user.getId())
                    .text(errorLocalization.getData())
                    .entities(errorLocalization.getEntities())
                    .replyMarkup(keyboardRemove)
                    .build();
        }
        LOGGER.debug("Compiling error message to user " + user.getId() + "...");
        return SendMessage.builder()
                .chatId(user.getId())
                .text(exc.getErrorLocalization().getData())
                .entities(exc.getErrorLocalization().getEntities())
                .replyMarkup(keyboardRemove)
                .build();
    }

    @Override
    public Class<? extends Exception> getExceptionClass() {
        return TelegramException.class;
    }
}
