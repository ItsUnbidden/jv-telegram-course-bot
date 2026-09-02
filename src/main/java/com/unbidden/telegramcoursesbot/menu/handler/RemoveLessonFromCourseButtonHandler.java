package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.LessonOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemoveLessonFromCourseButtonHandler extends AbstractButtonHandler {
    private static final String LESSON_ID_PARAM = "lessonId";

    private final ContentSessionService sessionService;

    private final ContentOrchestrationService contentService;

    private final LessonOrchestrationService lessonService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Lesson lesson = entityUtil.getLessonById(botRole, Long.parseLong(params.get(LESSON_ID_PARAM)));
        final String courseName = contentService.getLocalizedText(botRole, entityUtil.getCourseTitle(botRole, lesson.getCourse().getId()));
        final String confirmationPhrase = localizationLoader.localize(Localizations.Service.DELETE_LESSON_CONFIRMATION_PHRASE, botRole,
                new Localizations.Service.DeleteLessonConfirmationPhraseParams(courseName, lesson.getPosition())).getData();
  
        sessionService.createSession(botRole, p -> {
            lessonService.deleteLesson(p.botRole(), lesson.getId(), confirmationPhrase, p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.DELETE_LESSON_REQUEST,
                botRole, new Localizations.Service.DeleteLessonRequestParams(confirmationPhrase)));
    }
}
