package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemoveCourseButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(RemoveCourseButtonHandler.class);

    private static final String PARAM_COURSE_NAME = "${courseName}";

    private static final String SERVICE_REMOVE_COURSE_REQUEST = "service_remove_course_request";
    private static final String SERVICE_REMOVE_COURSE_SUCCESS = "service_remove_course_success";

    private static final long TEST_COURSE_ID = 1L;

    private final CourseService courseService;

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Course course = courseService.getCourseByName(params[0]);

            LOGGER.info("User " + user.getId() + " is trying to delete course "
                    + course.getName() + ".");
            if (course.getId().equals(TEST_COURSE_ID)) {
                throw new UnsupportedOperationException("Test course cannot be deleted");
            }
            sessionService.createSession(user, m -> {
                if (!m.get(0).hasText()) {
                    throw new InvalidDataSentException("A text message was expected");
                }
                final String providedStr = m.get(0).getText();
                LOGGER.debug("User has provided this string - " + providedStr
                        + ". Checking if this matches " + course.getName() + "...");
                if (!course.getName().equals(providedStr)) {
                    throw new InvalidDataSentException("Provided string does not match "
                            + "course name");
                }
                LOGGER.debug("Course name confirmed. Deleting course "
                        + course.getName() + "...");
                courseService.delete(course);
                LOGGER.info("Course " + course.getName() + " has been deleted.");
                LOGGER.debug("Sending confirmation message...");
                bot.sendMessage(user, localizationLoader.getLocalizationForUser(
                        SERVICE_REMOVE_COURSE_SUCCESS, user,
                        PARAM_COURSE_NAME, course.getName()));
                LOGGER.debug("Message sent.");
            });
            LOGGER.debug("Sending request message...");
            bot.sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_REMOVE_COURSE_REQUEST, user, PARAM_COURSE_NAME, course.getName()));
            LOGGER.debug("Message sent.");
        }
    }
}
