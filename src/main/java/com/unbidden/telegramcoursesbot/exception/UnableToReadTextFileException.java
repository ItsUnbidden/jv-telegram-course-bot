package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class UnableToReadTextFileException extends LocalizedException {
    public UnableToReadTextFileException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
