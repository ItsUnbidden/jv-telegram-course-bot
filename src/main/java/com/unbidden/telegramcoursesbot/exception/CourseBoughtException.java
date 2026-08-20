package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.localization.Localization;

public class CourseBoughtException extends LocalizedException {
    public CourseBoughtException(String message, Localization errorLoc) {
        super(message, errorLoc);
    }
}
