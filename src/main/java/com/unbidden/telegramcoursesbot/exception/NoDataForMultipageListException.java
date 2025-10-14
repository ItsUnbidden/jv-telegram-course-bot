package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.service.localization.Localization;

public class NoDataForMultipageListException extends LocalizedException {
    public NoDataForMultipageListException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
}
