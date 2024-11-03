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
public class UpdateHomeworkContentButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager
            .getLogger(UpdateHomeworkContentButtonHandler.class);

    private static final String PARAM_HOMEWORK_ID = "${homeworkId}";

    private static final String SERVICE_UPDATE_HOMEWORK_CONTENT_REQUEST =
            "service_update_homework_content_request";
    private static final String SERVICE_HOMEWORK_CONTENT_UPDATED =
            "service_homework_content_updated";

    private final ContentSessionService sessionService;

    private final HomeworkService homeworkService;

    private final LessonService lessonService;

    private final UserService userService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Long lessonId = Long.parseLong(params[2]);
            final Lesson lesson = lessonService.getById(lessonId);
            final Homework homework;

            LOGGER.debug("Trying to get homework for lesson " + lessonId + "...");
            if (lesson.getHomework() == null) {
                LOGGER.debug("Lesson does not have a homework yet. Creating...");
                homework = new Homework();
                homework.setAllowedMediaTypes("");
                homework.setFeedbackRequired(true);
                homework.setLesson(lesson);
                homework.setMapping(null); // TODO: this is not going to work. Create homework after the content is ready
                homework.setRepeatedCompletionAvailable(false);
                homeworkService.save(homework);
                lesson.setHomework(homework);
                lessonService.save(lesson);
                LOGGER.debug("New homework " + homework.getId() + " has been created.");
            } else {
                homework = lesson.getHomework();
                LOGGER.debug("Homework " + homework.getId() + " already exists.");
            }

            sessionService.createSession(user, m -> {
                LOGGER.info("User " + user.getId() + " is trying to update homework "
                        + homework.getId() + "...");  
                final String localizationName = "course_" + lesson.getCourse().getName()
                        + "_lesson_" + lesson.getPosition() + "_homework_content";
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
                            + "Assuming it to be user's telegram language.");
                    languageCode = user.getLanguageCode();
                }
                homeworkService.updateContent(homework.getId(), contentService
                        .parseAndPersistContent(m, localizationName, languageCode));
                LOGGER.info("Homework " + homework.getId() + " content has been updated.");
                LOGGER.debug("Sending confirmation message...");
                final Localization success = localizationLoader.getLocalizationForUser(
                        SERVICE_HOMEWORK_CONTENT_UPDATED, user, PARAM_HOMEWORK_ID,
                        homework.getId());
                bot.sendMessage(user, success);
                LOGGER.debug("Message sent.");
            });
            LOGGER.debug("Sending content request message...");
            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_UPDATE_HOMEWORK_CONTENT_REQUEST, user);
            bot.sendMessage(user, request);
            LOGGER.debug("Message sent.");
        }
    }
}
