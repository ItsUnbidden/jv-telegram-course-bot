package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseMaintenanceToggleButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            CourseMaintenanceToggleButtonHandler.class);

    private static final String PARAM_STATUS = "${status}";

    private static final String SERVICE_COURSE_MAINTENANCE_TOGGLE_SUCCESS =
            "service_course_maintenance_toggle_success";
    private static final String SERVICE_STATUS_DISABLED = "service_status_disabled";
    private static final String SERVICE_STATUS_ENABLED = "service_status_enabled";

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Course course = courseService.getCourseByName(params[0], user, bot);
        LOGGER.info("User " + user.getId() + " is trying to toggle maintenance for course "
                + course.getName() + "... Current status is " + getStatus(course) + ".");
        
        course.setUnderMaintenance(!course.isUnderMaintenance());
        courseService.save(course);
        
        LOGGER.info("Course " + course.getName() + "'s maintenance status is now "
                + getStatus(course) + ".");
        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_COURSE_MAINTENANCE_TOGGLE_SUCCESS, user, PARAM_STATUS,
                getStatus(user, course)));
        LOGGER.debug("Message sent.");
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
