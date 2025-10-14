package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemoveLessonFromCourseButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            AddLessonToCourseButtonHandler.class);

    private static final String SERVICE_DELETE_LESSON_REQUEST = "service_delete_lesson_request";
    private static final String SERVICE_LESSON_DELETED = "service_lesson_deleted";

    private static final String ERROR_LESSON_DELETE_CONFIRMATION_FAILURE =
            "error_lesson_delete_confirmation_failure";

    private static final int EXPECTED_MESSAGES = 1;

    private final ContentSessionService sessionService;

    private final LessonService lessonService;

    private final CourseService courseService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Course course = courseService.getCourseByName(params[0], user, bot);
        final Lesson lesson = lessonService.getById(Long.parseLong(params[2]), user, bot);
        LOGGER.info("User " + user.getId() + " is trying to remove lesson " + lesson.getId()
                + " from course " + course.getName() + "...");
        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user, m, localizationLoader);
            LOGGER.debug("Checking whether sent string matches course name...");
            if (!m.get(0).getText().trim().equals(course.getName())) {
                throw new InvalidDataSentException("Confirmation message does not match "
                        + "course name", localizationLoader.getLocalizationForUser(
                        ERROR_LESSON_DELETE_CONFIRMATION_FAILURE, user));
            }
            LOGGER.debug("Check passed. Deleting lesson...");
            lessonService.removeLesson(user, lesson);
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_LESSON_DELETED, user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_DELETE_LESSON_REQUEST, user));
        LOGGER.debug("Request sent.");
    }
}
