package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class CreateHomeworkButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(CreateHomeworkButtonHandler.class);
    
    private static final String PARAM_HOMEWORK_ID = "${homeworkId}";
    private static final String PARAM_LESSON_ID = "${lessonId}";

    private static final String SERVICE_HOMEWORK_CONTENT_REQUEST =
            "service_homework_content_request";
    private static final String SERVICE_NEW_HOMEWORK_CREATED = "service_new_homework_created";

    private final HomeworkService homeworkService;

    private final LessonService lessonService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final UserService userService;

    private final ContentService contentService;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Lesson lesson = lessonService.getById(Long.parseLong(params[2]));

            LOGGER.info("User " + user.getId() + " want to add a new homework to lesson "
                    + lesson.getId() + ".");
            sessionService.createSession(user, m -> {
                final String localizationName = "course_" + lesson.getCourse().getName()
                        + "_lesson_" + lesson.getPosition() + "_homework_content";
                LOGGER.debug("Homework localization in lesson " + lesson.getId()
                        + " will be " + localizationName);
                final Message lastMessage = m.get(m.size() - 1);
                final String languageCode;
                if (lastMessage.hasText()) {
                    if (lastMessage.getText().length() > 3) {
                        throw new InvalidDataSentException("Language code cannot be "
                                + "longer then 3 characters");
                    }
                    LOGGER.debug("Language code for new content will be "
                            + lastMessage.getText() + ".");
                    languageCode = lastMessage.getText();
                } else {
                    LOGGER.debug("Seems like language code is not specified. "
                            + "Assuming it to be user's Telegram language.");
                    languageCode = user.getLanguageCode();
                }
                final Homework homework = homeworkService.createDefault(lesson, contentService
                        .parseAndPersistContent(m, localizationName, languageCode));
                LOGGER.info("New Homework " + homework.getId() + " has been created for lesson "
                        + lesson.getId() + ".");
                LOGGER.debug("Sending confirmation message...");
                final Localization success = localizationLoader.getLocalizationForUser(
                        SERVICE_NEW_HOMEWORK_CREATED, user, PARAM_HOMEWORK_ID, homework.getId());
                bot.sendMessage(user, success);
                LOGGER.debug("Message sent.");
            });
            LOGGER.debug("Sending request message...");
            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_HOMEWORK_CONTENT_REQUEST, user, PARAM_LESSON_ID, lesson.getId());
            bot.sendMessage(user, request);
            LOGGER.debug("Message sent.");
        }
    }
}
