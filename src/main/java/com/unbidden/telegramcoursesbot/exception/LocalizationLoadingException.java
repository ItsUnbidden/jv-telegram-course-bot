package com.unbidden.telegramcoursesbot.exception;

public class LocalizationLoadingException extends RuntimeException {
    public LocalizationLoadingException(String message) {
        super(message);
    }

    public LocalizationLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
