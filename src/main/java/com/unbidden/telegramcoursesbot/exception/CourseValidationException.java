package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.localization.Localization;

public class CourseValidationException extends LocalizedException {
    public CourseValidationException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
}
