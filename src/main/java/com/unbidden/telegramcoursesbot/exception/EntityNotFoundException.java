package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class EntityNotFoundException extends LocalizedException {
    public EntityNotFoundException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
    
    public EntityNotFoundException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
