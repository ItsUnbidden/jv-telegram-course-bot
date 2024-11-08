package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class SessionException extends LocalizedException {
    public SessionException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }

    public SessionException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
