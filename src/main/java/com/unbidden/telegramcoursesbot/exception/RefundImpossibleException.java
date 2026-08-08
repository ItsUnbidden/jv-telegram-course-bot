package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.localization.Localization;

public class RefundImpossibleException extends LocalizedException {
    public RefundImpossibleException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
}
