package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.util.Blockable;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class FeedbackInclusionButtonHandler implements ButtonHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(FeedbackInclusionButtonHandler.class);

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    @Blockable
    public void handle(String[] params, User user) {
        final Course course = courseService.getCourseByName(params[0]);
        LOGGER.info("Feedback inclusion handler was triggered. Current value is: "
                + course.isFeedbackIncluded() + ".");

        course.setFeedbackIncluded(!course.isFeedbackIncluded());
        courseService.save(course);

        Map<String, Object> messageParams = new HashMap<>();
        messageParams.put("${status}", (course.isFeedbackIncluded()) ? "ENABLED" : "DISABLED");
        messageParams.put("${courseName}", course.getName());
        LOGGER.info("Value has been changed to: " + course.isFeedbackIncluded() + ".");
        Localization localization = localizationLoader.getLocalizationForUser(
            "service_course_feedback_update_successful", user, messageParams);
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .build());
    }
}
