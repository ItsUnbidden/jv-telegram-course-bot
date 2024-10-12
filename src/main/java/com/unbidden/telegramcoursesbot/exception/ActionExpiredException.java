package com.unbidden.telegramcoursesbot.exception;

public class ActionExpiredException extends RuntimeException {
    public ActionExpiredException(String msg) {
        super(msg);
    }
}
