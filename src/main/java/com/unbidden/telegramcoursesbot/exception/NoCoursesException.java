package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.localization.Localization;

public class NoCoursesException extends LocalizedException {
    public NoCoursesException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
}
