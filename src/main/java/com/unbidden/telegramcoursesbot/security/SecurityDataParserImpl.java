package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.exception.SecurityDataParsingException;
import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityDataParserImpl implements SecurityDataParser {
    private static final Logger LOGGER = LogManager.getLogger(SecurityDataParserImpl.class);

    private final UserService userService;

    @NonNull
    @Override
    public SecurityDto parse(@NonNull JoinPoint data) throws SecurityDataParsingException {
        MethodSignature signature = (MethodSignature)data.getSignature();
        Security annotation = signature.getMethod().getAnnotation(Security.class);
        String[] parameterNames = signature.getParameterNames();
        int botIndex = -1;
        int userIndex = -1;

        LOGGER.trace("Parsing of join point from security aspect is commencing. Method is "
                + signature.getName() + "...");
        final String userParamName = annotation.userParamName();
        final String botParamName = annotation.botParamName();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(botParamName)) {
                if (botIndex != -1) {
                    throw new SecurityDataParsingException("Method param " + i + " is "
                            + "named as bot param but bot param had already been found at index "
                            + botIndex);
                }
                botIndex = i;
                continue;
            }
            if (parameterNames[i].equals(userParamName)) {
                if (userIndex != -1) {
                    throw new SecurityDataParsingException("Method param " + i + " is "
                            + "named as user param but user param had already been found "
                            + "at index " + userIndex);
                }
                userIndex = i;
            }
        }

        if (botIndex == -1) {
            throw new SecurityDataParsingException("Method " + signature.getName() 
                    + " does not have a parameter with name " + botParamName);
        }
        if (userIndex == -1) {
            throw new SecurityDataParsingException("Method " + signature.getName() 
                    + " does not have a parameter with name " + userParamName);
        }
        
        Bot bot = null;
        Object entityArg = data.getArgs()[botIndex];
        if (entityArg instanceof Bot) {
            bot = (Bot)entityArg;
            LOGGER.trace("Bot " + bot.getId() + " has been aquired.");
        } else {
            throw new SecurityDataParsingException("Bot class type must be " + Bot.class.getName()
                    + " but currently is " + entityArg.getClass().getName());
        }

        UserEntity user = null;
        Object userArg = data.getArgs()[userIndex];
        if (userArg instanceof UserEntity) {
            user = (UserEntity)userArg;
            LOGGER.trace("User " + user.getId() + " has been aquired.");
        } else {
            throw new SecurityDataParsingException("User class type must be "
                    + UserEntity.class.getName() + " but currently is "
                    + userArg.getClass().getName());
        }

        LOGGER.trace("Fetching authorities...");
        final List<Authority> authorities = userService.parseAuthorities(
                annotation.authorities());
        LOGGER.trace("Authorities have been loaded.");

        LOGGER.trace("Data successfuly parsed. User id: " + user.getId()
                + "; bot id " + bot.getId());
        return new SecurityDto(bot, user, authorities);
    }
}
