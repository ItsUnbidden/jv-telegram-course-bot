package com.unbidden.telegramcoursesbot.exception;

public class UnableToReadTextFileException extends RuntimeException {
    public UnableToReadTextFileException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
