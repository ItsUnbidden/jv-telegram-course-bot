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
public class HomeworkInclusionButtonHandler implements ButtonHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(HomeworkInclusionButtonHandler.class);

    private final CourseRepository courseRepository;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(String[] params, User user) {
        final CourseModel course = courseRepository.findByName(params[0]).get();
        LOGGER.info("Homework inclusion handler was triggered. Current value is: "
                + course.isHomeworkIncluded() + ".");

        course.setHomeworkIncluded(!course.isHomeworkIncluded());
        courseRepository.save(course);
        LOGGER.info("Value has been changed to: " + course.isHomeworkIncluded() + ".");
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(localizationLoader.getTextByNameForUser(
                    "message_course_homework_update_successful", user)
                    + course.isHomeworkIncluded())
                .build());
    }
}
