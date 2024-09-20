package com.unbidden.telegramcoursesbot.exception;

public class TelegramException extends RuntimeException {
    public TelegramException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
