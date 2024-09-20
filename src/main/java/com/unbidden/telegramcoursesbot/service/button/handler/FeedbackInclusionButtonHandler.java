package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.dao.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.CourseModel;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
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

    private final CourseRepository courseRepository;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(String[] params, User user) {
        final CourseModel course = courseRepository.findByName(params[0]).get();
        LOGGER.info("Feedback inclusion handler was triggered. Current value is: "
                + course.isFeedbackIncluded() + ".");

        course.setFeedbackIncluded(!course.isFeedbackIncluded());
        courseRepository.save(course);
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(localizationLoader.getTextByNameForUser(
                    "message_course_feedback_update_successful", user)
                    + course.isFeedbackIncluded())
                .build());
    }
}
