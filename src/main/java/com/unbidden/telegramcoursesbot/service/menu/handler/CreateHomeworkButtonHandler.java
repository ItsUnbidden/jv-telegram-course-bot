package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class CreateHomeworkButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(CreateHomeworkButtonHandler.class);
    
    private static final String PARAM_HOMEWORK_ID = "${homeworkId}";
    private static final String PARAM_LESSON_ID = "${lessonId}";

    private static final String SERVICE_HOMEWORK_CONTENT_REQUEST =
            "service_homework_content_request";
    private static final String SERVICE_NEW_HOMEWORK_CREATED = "service_new_homework_created";

    private static final String ERROR_LANGUAGE_CODE_LENGTH = "error_language_code_length";

    private static final String COURSE_LESSON_CONTENT = "course_%s_lesson_%s_homework_content";

    private final HomeworkService homeworkService;

    private final LessonService lessonService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ContentService contentService;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Lesson lesson = lessonService.getById(Long.parseLong(params[2]), user, bot);

        LOGGER.info("User " + user.getId() + " want to add a new homework to lesson "
                + lesson.getId() + ".");
        sessionService.createSession(user, bot, m -> {
            final String localizationName = COURSE_LESSON_CONTENT.formatted(
                    lesson.getCourse().getName(), lesson.getPosition());
            LOGGER.debug("Homework localization in lesson " + lesson.getId()
                    + " will be " + localizationName);
            final Message lastMessage = m.get(m.size() - 1);
            final String languageCode;

            if (lastMessage.hasText()) {
                if (lastMessage.getText().length() > 3
                        || lastMessage.getText().length() < 2) {
                    throw new InvalidDataSentException("Language code must be "
                            + "between 2 and 3 characters", localizationLoader
                            .getLocalizationForUser(ERROR_LANGUAGE_CODE_LENGTH, user));
                }
                LOGGER.debug("Language code for new content will be "
                        + lastMessage.getText() + ".");
                languageCode = lastMessage.getText();
            } else {
                languageCode = user.getLanguageCode();
                LOGGER.debug("Seems like language code is not specified. "
                        + "Assuming it to be user's telegram language which is "
                        + languageCode + ".");
            }
            final Homework homework = homeworkService.createDefault(lesson, contentService
                    .parseAndPersistContent(bot, m, localizationName, languageCode));
            LOGGER.info("New Homework " + homework.getId() + " has been created for lesson "
                    + lesson.getId() + ".");
            LOGGER.debug("Sending confirmation message...");
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_NEW_HOMEWORK_CREATED, user, PARAM_HOMEWORK_ID, homework.getId());
            clientManager.getClient(bot).sendMessage(user, success);
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending request message...");
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_HOMEWORK_CONTENT_REQUEST, user, PARAM_LESSON_ID, lesson.getId());
        clientManager.getClient(bot).sendMessage(user, request);
        LOGGER.debug("Message sent.");
    }
}
