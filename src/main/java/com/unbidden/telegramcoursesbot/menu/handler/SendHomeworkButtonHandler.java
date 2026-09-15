package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendHomeworkButtonHandler extends AbstractButtonHandler {
    private static final String PROGRESS_ID_PARAM = "progressId";

    private final HomeworkOrchestrationService homeworkService;

    private final CourseOrchestrationService courseService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.LAUNCH_COURSE)
    public void handle(BotRole botRole, Map<String, String> params) {
        final HomeworkProgress progress = entityUtil.getHomeworkProgressById(botRole, Long.parseLong(params.get(PROGRESS_ID_PARAM)));

        courseService.checkCourseIsNotUnderMaintenance(botRole, progress.getHomework().getLesson().getCourse().getId());

        sessionService.createSession(botRole, p -> {
            homeworkService.commit(p.botRole(), progress.getHomework().getId(), p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.SEND_HOMEWORK_REQUEST, botRole));
    }
}
