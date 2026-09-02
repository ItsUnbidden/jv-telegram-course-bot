package com.unbidden.telegramcoursesbot.exception;

import com.unbidden.telegramcoursesbot.localization.Localization;

import lombok.Getter;

@Getter
public class RefundImpossibleException extends Exception {
    private final Localization loc;

    public RefundImpossibleException(String msg, Localization errorLoc) {
        super(msg);
        this.loc = errorLoc;
    }
}
