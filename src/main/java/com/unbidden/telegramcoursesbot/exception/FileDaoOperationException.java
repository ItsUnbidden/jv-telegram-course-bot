package com.unbidden.telegramcoursesbot.exception;

public class FileDaoOperationException extends RuntimeException {
    public FileDaoOperationException(String msg) {
        super(msg);
    }
    
    public FileDaoOperationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
