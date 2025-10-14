package com.unbidden.telegramcoursesbot.exception;

public class CallbackQueryAnswerException extends Exception {
    public CallbackQueryAnswerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
