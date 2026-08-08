package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.localization.Localization;

public class ActionExpiredException extends LocalizedException {
    public ActionExpiredException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
}
