package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.LessonOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateContentPositionButtonHandler extends AbstractButtonHandler {
    private static final String LESSON_ID_PARAM = "lessonId";

    private final ContentSessionService sessionService;

    private final LessonOrchestrationService lessonService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        sessionService.createSession(user, bot, p -> {
            lessonService.moveMappingToIndex(p.user(), p.bot(), Long.parseLong(params.get(LESSON_ID_PARAM)), p.messages());
        });

        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.LESSON_MAPPING_ORDER_CHANGE_REQUEST, user));
    }
}
