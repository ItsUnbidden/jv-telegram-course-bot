package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class InvalidDataSentException extends LocalizedException {
    public InvalidDataSentException(String message, Localization errorLoc) {
        super(message, errorLoc);
    }

    public InvalidDataSentException(String message, Localization errorLoc, Throwable cause) {
        super(message, errorLoc, cause);
    }
}
