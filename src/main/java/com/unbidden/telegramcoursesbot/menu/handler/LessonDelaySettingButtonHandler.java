package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.LessonOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonDelaySettingButtonHandler extends AbstractButtonHandler {
    private static final String LESSON_ID_PARAM = "lessonId";

    private final LessonOrchestrationService lessonService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Long lessonId = Long.parseLong(params.get(LESSON_ID_PARAM));

        sessionService.createSession(user, bot, p -> {
            lessonService.updateDelay(p.user(), p.bot(), lessonId, p.messages());
        }, true);
        
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .localize(Localizations.Service.NEW_LESSON_DELAY_REQUEST, user,
                new Localizations.Service.NewLessonDelayRequestParams(LessonOrchestrationService.MAX_LESSON_DELAY)));
    }
}
