package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendHomeworkButtonHandler extends AbstractButtonHandler {
    private static final String SERVICE_SEND_HOMEWORK_REQUEST = "service_send_homework_request";

    private final HomeworkService homeworkService;

    private final CourseService courseService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.LAUNCH_COURSE)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final HomeworkProgress progress = homeworkService.getProgress(Long.parseLong(params[0]),
                user);
        courseService.checkCourseIsNotUnderMaintenance(progress.getHomework().getLesson()
                .getCourse(), user);

        sessionService.createSession(user, bot, m ->
                homeworkService.commit(progress, m));

        final Localization localization = localizationLoader.localize(
                SERVICE_SEND_HOMEWORK_REQUEST, user);
        
        clientManager.getClient(bot).sendMessage(user, localization);
    }
}
