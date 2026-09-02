package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.exception.SecurityDataParsingException;
import com.unbidden.telegramcoursesbot.model.BotRole;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityDataParserImpl implements SecurityDataParser {
    private static final Logger LOGGER = LogManager.getLogger(SecurityDataParserImpl.class);

    @Override
    public SecurityDto parse(JoinPoint data) throws SecurityDataParsingException {
        MethodSignature signature = (MethodSignature)data.getSignature();
        Security annotation = signature.getMethod().getAnnotation(Security.class);
        String[] parameterNames = signature.getParameterNames();
        int botRoleIndex = -1;

        LOGGER.trace("Parsing of join point from security aspect is commencing. Method is "
                + signature.getName() + "...");
        final String botRoleParamName = annotation.botRoleParamName();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(botRoleParamName)) {
                if (botRoleIndex != -1) {
                    throw new SecurityDataParsingException("Method param " + i + " is "
                            + "named as the bot role param but the bot role param had already been found at index "
                            + botRoleIndex);
                }
                botRoleIndex = i;
                continue;
            }
        }

        if (botRoleIndex == -1) {
            throw new SecurityDataParsingException("Method " + signature.getName() 
                    + " does not have a parameter with name " + botRoleParamName);
        }
        
        Object botRoleArg = data.getArgs()[botRoleIndex];
        if (botRoleArg instanceof BotRole botRole) {

            LOGGER.trace("Data successfuly parsed. Bot role id is " + botRole.getId() + ".");
            return new SecurityDto(botRole, List.of(annotation.authorities()), annotation.isBotLordOnly());
        } else {
            throw new SecurityDataParsingException("Bot role class type must be " + BotRole.class.getName()
                    + " but the supplied value is of class " + botRoleArg.getClass().getName());
        }
    }
}
