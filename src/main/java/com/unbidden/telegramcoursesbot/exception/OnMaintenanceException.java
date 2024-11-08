package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class OnMaintenanceException extends LocalizedException {
    public OnMaintenanceException(String message, Localization errorLoc) {
        super(message, errorLoc);
    }
}
