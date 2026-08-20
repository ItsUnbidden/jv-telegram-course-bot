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
public class AddContentToLessonButtonHandler extends AbstractButtonHandler {
    private static final String LESSON_ID_PARAM = "lessonId";

    private final ContentSessionService sessionService;

    private final LessonOrchestrationService lessonService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;
    
    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Long lessonId = Long.parseLong(params.get(LESSON_ID_PARAM));

        sessionService.createSession(user, bot, p -> {
            lessonService.addContentToLesson(p.user(), p.bot(), lessonId, p.messages());
        });

        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.ADD_LESSON_CONTENT_REQUEST, user,
                new Localizations.Service.AddLessonContentRequestParams(lessonId)));
    }
}
