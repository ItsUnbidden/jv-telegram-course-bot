package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class FeedbackInclusionButtonHandler implements ButtonHandler {
    private static final String SERVICE_COURSE_FEEDBACK_UPDATE_SUCCESSFUL =
            "service_course_feedback_update_successful";

    private static final Logger LOGGER =
            LogManager.getLogger(FeedbackInclusionButtonHandler.class);

    private final CourseService courseService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    @Blockable
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Course course = courseService.getCourseByName(params[0]);
            LOGGER.info("Feedback inclusion handler was triggered. Current value is: "
                    + course.isFeedbackIncluded() + ".");

            course.setFeedbackIncluded(!course.isFeedbackIncluded());
            courseService.save(course);

            Map<String, Object> messageParams = new HashMap<>();
            messageParams.put("${status}", (course.isFeedbackIncluded()) ? "ENABLED"
                    : "DISABLED");
            messageParams.put("${courseName}", course.getName());
            LOGGER.info("Value has been changed to: " + course.isFeedbackIncluded() + ".");
            Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_COURSE_FEEDBACK_UPDATE_SUCCESSFUL, user, messageParams);
            bot.sendMessage(SendMessage.builder()
                    .chatId(user.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        }
    }
}
