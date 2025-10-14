package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.exception.SecurityDataParsingException;
import org.aspectj.lang.JoinPoint;
import org.springframework.lang.NonNull;

/**
 * This class contains a single {@link #parse} method that parses raw data from {@link JoinPoint}
 * to {@link SecurityDto}.
 * @author Unbidden
 */
public interface SecurityDataParser {
    /**
     * Parses {@link JoinPoint} raw method data into a convinient to access
     * {@link SecurityDto} object.
     * @param data from the method
     * @return {@link SecurityDto} object with parsed data
     */
    @NonNull
    SecurityDto parse(@NonNull JoinPoint data) throws SecurityDataParsingException;
}
