package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateHomeworkContentButtonHandler extends AbstractButtonHandler {
    private static final String HOMEWORK_ID_PARAM = "homeworkId";
    private static final String LESSON_ID_PARAM = "lessonId";

    private final ContentSessionService sessionService;

    private final HomeworkOrchestrationService homeworkService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Long homeworkId = Long.parseLong(params.get(HOMEWORK_ID_PARAM));
        final Long lessonId = Long.parseLong(params.get(LESSON_ID_PARAM));

        sessionService.createSession(user, bot, p -> {
            homeworkService.updateContent(p.user(), p.bot(), homeworkId, p.messages());
        });

        final Localization request = localizationLoader.localize(
                Localizations.Service.HOMEWORK_CONTENT_REQUEST, user,
                new Localizations.Service.HomeworkContentRequestParams(lessonId));
        clientManager.getClient(bot).sendMessage(user, request);
    }
}
