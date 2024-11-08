package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class ArchiveReviewsException extends LocalizedException {
    public ArchiveReviewsException(String message, Localization errorLoc, Throwable cause) {
        super(message, errorLoc, cause);
    }
}
