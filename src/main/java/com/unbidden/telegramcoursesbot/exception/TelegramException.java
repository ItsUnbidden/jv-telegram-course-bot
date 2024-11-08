package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class TelegramException extends LocalizedException {
    public TelegramException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
