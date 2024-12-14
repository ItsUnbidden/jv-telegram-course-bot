package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.exception.LocalizedException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

@Component
public class GeneralLocalizedExceptionHandler implements LocalizedExceptionHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(GeneralLocalizedExceptionHandler.class);

    private static final String PARAM_EXC_MESSAGE = "${excMessage}";
    private static final String PARAM_EXC_CLASS_NAME = "${excClassName}";

    private static final String ERROR_NO_EXCEPTION_LOCALIZATION_AVAILABLE =
            "error_no_exception_localization_available";

    @Autowired
    protected LocalizationLoader localizationLoader;

    @Autowired
    protected ReplyKeyboardRemove keyboardRemove;

    @Override
    public SendMessage compileSendMessageFromLocalizedExc(@NonNull UserEntity user,
            @NonNull Bot bot, @NonNull LocalizedException exc) {
        LOGGER.debug("User " + user.getId() + " has triggered an exception: ", exc);

        if (exc.getErrorLocalization() == null) {
            final Map<String, Object> parameterMap = new HashMap<>();

            parameterMap.put(PARAM_EXC_MESSAGE, exc.getMessage());
            parameterMap.put(PARAM_EXC_CLASS_NAME, exc.getClass().getSimpleName());

            final Localization errorLocalization = localizationLoader.getLocalizationForUser(
                    ERROR_NO_EXCEPTION_LOCALIZATION_AVAILABLE, user, parameterMap);
            LOGGER.debug("There is no localization available for error message. "
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
        return LocalizedException.class;
    }
}
