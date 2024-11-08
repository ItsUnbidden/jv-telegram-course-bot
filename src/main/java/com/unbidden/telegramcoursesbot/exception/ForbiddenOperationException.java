package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class ForbiddenOperationException extends LocalizedException {
    public ForbiddenOperationException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
    
    public ForbiddenOperationException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
