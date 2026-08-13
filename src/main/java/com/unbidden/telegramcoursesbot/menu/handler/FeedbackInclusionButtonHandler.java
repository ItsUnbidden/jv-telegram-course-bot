package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedbackInclusionButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(FeedbackInclusionButtonHandler.class);

    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_STATUS = "${status}";

    private static final String SERVICE_COURSE_FEEDBACK_UPDATE_SUCCESS =
            "service_course_feedback_update_success";
    private static final String SERVICE_STATUS_DISABLED = "service_status_disabled";
    private static final String SERVICE_STATUS_ENABLED = "service_status_enabled";

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Course course = courseService.getCourseByName(params[0], user, bot);
        LOGGER.info("Feedback inclusion handler was triggered. Current status is: "
                + getStatus(course) + ".");

        course.setFeedbackIncluded(!course.isFeedbackIncluded());
        courseService.save(course);

        Map<String, Object> messageParams = new HashMap<>();
        messageParams.put(PARAM_STATUS, getStatus(user, course));
        messageParams.put(PARAM_COURSE_NAME, course.getName());
        LOGGER.info("Status has been changed to: " + getStatus(course) + ".");
        Localization localization = localizationLoader.localize(
            SERVICE_COURSE_FEEDBACK_UPDATE_SUCCESS, user, messageParams);
        clientManager.getClient(bot).sendMessage(user, localization);
    }

    private String getStatus(UserEntity user, Course course) {
        return (course.isUnderMaintenance()) ? localizationLoader
                .localize(SERVICE_STATUS_ENABLED, user).getData()
                : localizationLoader.localize(SERVICE_STATUS_DISABLED, user)
                .getData();
    }

    private String getStatus(Course course) {
        return (course.isUnderMaintenance()) ? "ENABLED" : "DISABLED";
    }
}
