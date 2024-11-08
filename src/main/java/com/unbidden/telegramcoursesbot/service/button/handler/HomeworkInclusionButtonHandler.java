package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.Blockable;
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

    private static final String SERVICE_COURSE_HOMEWORK_UPDATE_SUCCESSFUL =
            "service_course_homework_update_successful";

    private final CourseService courseService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    @Blockable
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Course course = courseService.getCourseByName(params[0], user);
            LOGGER.info("Homework inclusion handler was triggered. Current value is: "
                    + course.isHomeworkIncluded() + ".");
    
            course.setHomeworkIncluded(!course.isHomeworkIncluded());
            courseService.save(course);
    
            Map<String, Object> messageParams = new HashMap<>();
            messageParams.put(PARAM_STATUS, (course.isHomeworkIncluded()) ? "ENABLED"
                    : "DISABLED");
            messageParams.put("${courseName}", course.getName());
            LOGGER.info("Value has been changed to: " + course.isHomeworkIncluded() + ".");
            Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_COURSE_HOMEWORK_UPDATE_SUCCESSFUL, user, messageParams);
            client.sendMessage(user, localization);
        }
    }
}
