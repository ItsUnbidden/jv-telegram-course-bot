package com.unbidden.telegramcoursesbot.exception;

public class SessionException extends RuntimeException {
    public SessionException(String msg) {
        super(msg);
    }

    public SessionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
