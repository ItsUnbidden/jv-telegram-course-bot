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
public class AcceptHomeworkButtonHandler extends AbstractButtonHandler {
    private static final String ACCEPT_HOMEWORK_WITH_COMMENT_BUTTON = "ahwc";

    private static final String SERVICE_APPROVE_HOMEWORK_COMMENT_REQUEST =
            "service_approve_homework_comment_request";

    private final HomeworkService homeworkService;

    private final CourseService courseService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.GIVE_HOMEWORK_FEEDBACK)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final HomeworkProgress progress = homeworkService.getProgress(Long.parseLong(params[0]),
                user);
        courseService.checkCourseIsNotUnderMaintenance(progress.getHomework().getLesson()
                .getCourse(), user);
        switch (params[params.length - 1]) {
            case ACCEPT_HOMEWORK_WITH_COMMENT_BUTTON:
                sessionService.createSession(user, bot, m -> homeworkService.approve(
                        progress, user, m));
                        
                final Localization localization = localizationLoader.localize(
                        SERVICE_APPROVE_HOMEWORK_COMMENT_REQUEST, user);
                clientManager.getClient(bot).sendMessage(user, localization);
                break;
            default:
                homeworkService.approve(progress, user, null);
                break;
        }
    }
}
