package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class FileDaoOperationException extends LocalizedException {
    public FileDaoOperationException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
    
    public FileDaoOperationException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
