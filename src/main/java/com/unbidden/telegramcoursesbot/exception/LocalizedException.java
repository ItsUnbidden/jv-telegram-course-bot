package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class LocalizedException extends RuntimeException {
    private Localization errorLocalization;

    public LocalizedException(String msg, Localization errorLoc) {
        super(msg);
        this.errorLocalization = errorLoc;
    }

    public LocalizedException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, cause);
        this.errorLocalization = errorLoc;
    }

    public Localization getErrorLocalization() {
        return errorLocalization;
    }
}
