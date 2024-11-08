package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class LocalizationLoadingException extends LocalizedException {
    public LocalizationLoadingException(String message, Localization errorLoc) {
        super(message, errorLoc);
    }

    public LocalizationLoadingException(String message, Localization errorLoc, Throwable cause) {
        super(message, errorLoc, cause);
    }
}
