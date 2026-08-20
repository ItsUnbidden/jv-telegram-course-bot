package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
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
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));
        final String courseName = contentService.getLocalizedText(user, bot, entityUtil.getCourseTitle(user, bot, courseId));
        final String confirmationPhrase = localizationLoader.localize(Localizations.Service.DELETE_COURSE_CONFIRMATION_PHRASE, user,
                new Localizations.Service.DeleteCourseConfirmationPhraseParams(courseName)).getData();

        sessionService.createSession(user, bot, p -> {
            courseService.deleteCourse(p.user(), p.bot(), courseId, confirmationPhrase, p.messages());
        });

        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .localize(Localizations.Service.DELETE_COURSE_REQUEST, user,
                    new Localizations.Service.DeleteCourseRequestParams(confirmationPhrase)));
    }
}
