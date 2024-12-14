package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeworkInclusionButtonHandler implements ButtonHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(HomeworkInclusionButtonHandler.class);
            
    private static final String PARAM_STATUS = "${status}";
    private static final String PARAM_COURSE_NAME = "${courseName}";

    private static final String SERVICE_COURSE_HOMEWORK_UPDATE_SUCCESS =
            "service_course_homework_update_success";
    private static final String SERVICE_STATUS_DISABLED = "service_status_disabled";
    private static final String SERVICE_STATUS_ENABLED = "service_status_enabled";

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Course course = courseService.getCourseByName(params[0], user, bot);
        LOGGER.info("Homework inclusion handler was triggered. Current status is: "
                + getStatus(course) + ".");

        course.setHomeworkIncluded(!course.isHomeworkIncluded());
        courseService.save(course);

        Map<String, Object> messageParams = new HashMap<>();
        messageParams.put(PARAM_STATUS, getStatus(user, course));
        messageParams.put(PARAM_COURSE_NAME, course.getName());
        LOGGER.info("Status has been changed to: " + getStatus(course) + ".");
        Localization localization = localizationLoader.getLocalizationForUser(
            SERVICE_COURSE_HOMEWORK_UPDATE_SUCCESS, user, messageParams);
        clientManager.getClient(bot).sendMessage(user, localization);
    }

    private String getStatus(UserEntity user, Course course) {
        return (course.isUnderMaintenance()) ? localizationLoader
                .getLocalizationForUser(SERVICE_STATUS_ENABLED, user).getData()
                : localizationLoader.getLocalizationForUser(SERVICE_STATUS_DISABLED, user)
                .getData();
    }

    private String getStatus(Course course) {
        return (course.isUnderMaintenance()) ? "ENABLED" : "DISABLED";
    }
}
