package com.unbidden.telegramcoursesbot.exception;

public class InvalidDataSentException extends RuntimeException {
    public InvalidDataSentException(String message) {
        super(message);
    }

    public InvalidDataSentException(String message, Throwable cause) {
        super(message, cause);
    }
}
