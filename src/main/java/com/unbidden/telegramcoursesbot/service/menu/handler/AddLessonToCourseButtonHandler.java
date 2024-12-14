package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddLessonToCourseButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            AddLessonToCourseButtonHandler.class);

    private static final String SERVICE_CREATE_LESSON_REQUEST = "service_create_lesson_request";
    private static final String SERVICE_NEW_LESSON_CREATED = "service_new_lesson_created";

    private static final String ERROR_LESSON_POSITION_INVALID = "error_lesson_position_invalid";
    private static final String ERROR_PARSE_INDEX_FAILURE = "error_parse_index_failure";
    
    private static final int EXPECTED_MESSAGES = 1;

    private final ContentSessionService sessionService;

    private final LessonService lessonService;

    private final CourseService courseService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Course course = courseService.getCourseByName(params[0], user, bot);
        LOGGER.info("User " + user.getId() + " is trying to add a new lesson in course "
                + course.getName() + "...");
        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user, m, localizationLoader);
            final int position;
            try {
                position = Integer.parseInt(m.get(0).getText());
                if (position < 0 || position > course.getAmountOfLessons()) {
                    throw new InvalidDataSentException("Position " + position + " is anvailable "
                            + "for course " + course.getName() + " because it is outside "
                            + "allowed boundaries of 0 to " + course.getAmountOfLessons(),
                            localizationLoader.getLocalizationForUser(
                            ERROR_LESSON_POSITION_INVALID, user));
                }
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse string "
                        + m.get(1).getText() + " to the new index", localizationLoader
                        .getLocalizationForUser(ERROR_PARSE_INDEX_FAILURE, user));
            }
            LOGGER.debug("New position parsed. Adding lesson...");
            lessonService.createLesson(user, course, position);
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_NEW_LESSON_CREATED, user));
            LOGGER.debug("Message sent.");
        }, true);
        LOGGER.debug("Sending request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_CREATE_LESSON_REQUEST, user));
        LOGGER.debug("Request sent.");
    }
}
