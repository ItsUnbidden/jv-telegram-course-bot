package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class AccessDeniedException extends LocalizedException {
    public AccessDeniedException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }

    public AccessDeniedException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
