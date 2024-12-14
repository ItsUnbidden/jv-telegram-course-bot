package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.exception.SecurityDataParsingException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {
    private static final Logger LOGGER = LogManager.getLogger(SecurityAspect.class);
    
    private final SecurityDataParser dataParser;

    private final SecurityService securityService;
    
    @Around("@annotation(com.unbidden.telegramcoursesbot.security.Security)")
    public void projectAccessAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.trace("Project security aspect commencing...");
        SecurityDto dataFromJoinPoint;
        try {
            dataFromJoinPoint = dataParser.parse(joinPoint);
        } catch (SecurityDataParsingException e) {
            throw new RuntimeException("Cannot continue without resolving parsing issue", e);
        }

        if (securityService.grantAccess(dataFromJoinPoint.getBot(), dataFromJoinPoint.getUser(),
                dataFromJoinPoint.getAuthorities())) {
            joinPoint.proceed();
        } else {
            LOGGER.debug("Cannot proceed to the method since access was denied.");
        }
    }
}
