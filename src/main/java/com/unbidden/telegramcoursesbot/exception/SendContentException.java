package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.localization.Localization;

public class SendContentException extends LocalizedException {
    public SendContentException(String msg, Localization errorLoc, Throwable cause) {
        super(msg, errorLoc, cause);
    }
}
