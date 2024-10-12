package com.unbidden.telegramcoursesbot.exception;

public class CourseIsAlreadyOwnedException extends Exception {
    public CourseIsAlreadyOwnedException(String message) {
        super(message);
    }
}
