package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.exception.MenuException;
import com.unbidden.telegramcoursesbot.localization.Localization;

public class StaleMenuException extends MenuException {
    public StaleMenuException(String msg, Localization errorLoc) {
        super(msg, errorLoc);
    }
}
