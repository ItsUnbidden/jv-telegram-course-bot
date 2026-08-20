package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.UserOrchestrationService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectLanguageButtonHandler extends AbstractButtonHandler {
    private static final String LANGUAGE_CODE_PARAM = "terminal";

    private final UserOrchestrationService userService;

    @Override
    @Security(authorities = AuthorityType.INFO)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final String potentialCode = params.get(LANGUAGE_CODE_PARAM);

        if (potentialCode == null) {
            userService.resetLanguageToDefault(user, bot);
            
        } else {
            userService.changeLanguage(user, bot, potentialCode);
            
        }
    }
}
