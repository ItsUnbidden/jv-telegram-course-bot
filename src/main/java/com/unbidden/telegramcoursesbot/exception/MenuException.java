package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.localization.Localization;

public class MenuException extends LocalizedException {
    public MenuException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }

    public MenuException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
