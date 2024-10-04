package com.unbidden.telegramcoursesbot.exception;

public class InvalidDataSentException extends RuntimeException {
    public InvalidDataSentException(String message) {
        super(message);
    }
}
