package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemoveCourseButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final CourseOrchestrationService courseService;

    private final ContentOrchestrationService contentService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));
        final String courseName = contentService.getLocalizedText(botRole, entityUtil.getCourseTitle(botRole, courseId));
        final String confirmationPhrase = localizationLoader.localize(Localizations.Service.DELETE_COURSE_CONFIRMATION_PHRASE, botRole,
                new Localizations.Service.DeleteCourseConfirmationPhraseParams(courseName)).getData();

        sessionService.createSession(botRole, p -> {
            courseService.deleteCourse(p.botRole(), courseId, confirmationPhrase, p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.DELETE_COURSE_REQUEST,
                botRole, new Localizations.Service.DeleteCourseRequestParams(confirmationPhrase)));
    }
}
