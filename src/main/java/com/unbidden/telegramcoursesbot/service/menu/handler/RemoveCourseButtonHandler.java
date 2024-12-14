package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
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
public class RemoveCourseButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(RemoveCourseButtonHandler.class);
    
    private static final String PARAM_COURSE_NAME = "${courseName}";
    
    private static final String SERVICE_REMOVE_COURSE_REQUEST = "service_remove_course_request";
    private static final String SERVICE_REMOVE_COURSE_SUCCESS = "service_remove_course_success";
    
    private static final String ERROR_COURSE_REMOVE_CONFIRMATION_FAILURE =
            "error_course_remove_confirmation_failure";
    private static final String ERROR_DELETE_TEST_COURSE_FAILURE =
            "error_delete_test_course_failure";
    private static final String ERROR_CANNOT_DELETE_BOUGHT_COURSE =
            "error_cannot_delete_bought_course";

    private static final long TEST_COURSE_ID = 1L;
    private static final int EXPECTED_MESSAGES = 1;

    private final CourseService courseService;

    private final ContentSessionService sessionService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Course course = courseService.getCourseByName(params[0], user, bot);

        LOGGER.info("User " + user.getId() + " is trying to delete course "
                + course.getName() + ".");
        if (course.getId().equals(TEST_COURSE_ID)) {
            throw new ForbiddenOperationException("Test course cannot be deleted",
                    localizationLoader.getLocalizationForUser(
                    ERROR_DELETE_TEST_COURSE_FAILURE, user));
        }
        if (!courseService.isDeletable(course)) {
            throw new ForbiddenOperationException("Cannot delete a course which has been "
                    + "bought by users before", localizationLoader.getLocalizationForUser(
                    ERROR_CANNOT_DELETE_BOUGHT_COURSE, user));
        }
        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user, m, localizationLoader);
            final String providedStr = m.get(0).getText();
            LOGGER.debug("User has provided this string - " + providedStr
                    + ". Checking if this matches " + course.getName() + "...");
            if (!course.getName().equals(providedStr)) {
                throw new InvalidDataSentException("Provided string does not match "
                        + "course name", localizationLoader.getLocalizationForUser(
                        ERROR_COURSE_REMOVE_CONFIRMATION_FAILURE, user));
            }
            LOGGER.debug("Course name confirmed. Deleting course "
                    + course.getName() + "...");
            courseService.delete(course, user);
            LOGGER.info("Course " + course.getName() + " has been deleted.");
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_REMOVE_COURSE_SUCCESS, user,
                    PARAM_COURSE_NAME, course.getName()));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_REMOVE_COURSE_REQUEST, user,
                PARAM_COURSE_NAME, course.getName()));
        LOGGER.debug("Message sent.");
    }
}
