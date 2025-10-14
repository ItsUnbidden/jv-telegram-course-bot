package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeclineHomeworkButtonHandler implements ButtonHandler {
    private static final String SERVICE_DECLINE_HOMEWORK_COMMENT_REQUEST =
            "service_decline_homework_comment_request";

    private final HomeworkService homeworkService;
    
    private final CourseService courseService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.GIVE_HOMEWORK_FEEDBACK)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final HomeworkProgress progress = homeworkService.getProgress(Long.parseLong(params[0]),
                user);
        courseService.checkCourseIsNotUnderMaintenance(progress.getHomework().getLesson()
                .getCourse(), user);
                
        sessionService.createSession(user, bot, m ->
                homeworkService.decline(progress, user, m));

        final Localization localization = localizationLoader.getLocalizationForUser(
                        SERVICE_DECLINE_HOMEWORK_COMMENT_REQUEST, user);
        clientManager.getClient(bot).sendMessage(user, localization);
    }
}
