package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.Lesson.SequenceOption;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateCourseButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(CreateCourseButtonHandler.class);

    private static final String PARAM_MAX_PRICE = "${maxPrice}";
    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_MESSAGE_INDEX = "${messageIndex}";
    private static final String PARAM_AMOUNT_OF_LESSONS = "${amountOfLessons}";
    private static final String PARAM_PROVIDED_MESSAGES_AMOUNT = "${providedMessagesNumber}";
    private static final String PARAM_EXPECTED_MESSAGES_AMOUNT = "${expectedMessagesAmount}";

    private static final String SERVICE_NEW_COURSE_REQUEST = "service_new_course_request";
    private static final String SERVICE_NEW_COURSE_CREATED = "service_new_course_created";
    
    private static final String ERROR_PRICE_LIMIT = "error_price_limit";
    private static final String ERROR_NEW_COURSE_WRONG_LESSONS_AMOUNT =
            "error_new_course_wrong_lessons_amount";
    private static final String ERROR_MESSAGE_TEXT_MISSING = "error_message_text_missing";
    private static final String ERROR_AMOUNT_OF_MESSAGES = "error_amount_of_messages";
    
    private static final int MAX_PRICE = 100_000;

    private static final int NUMBER_OF_MESSAGES_EXPECTED = 3;

    private final ContentSessionService sessionService;

    private final CourseService courseService;

    private final LessonService lessonService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            sessionService.createSession(user, m -> {
                LOGGER.info("User " + user.getId() + " is trying to create a new course. "
                        + "Running data checks...");  
                if (m.size() != NUMBER_OF_MESSAGES_EXPECTED) {
                    final Map<String, Object> parameterMap = new HashMap<>();
                    parameterMap.put(PARAM_EXPECTED_MESSAGES_AMOUNT, NUMBER_OF_MESSAGES_EXPECTED);
                    parameterMap.put(PARAM_PROVIDED_MESSAGES_AMOUNT, m.size());

                    throw new InvalidDataSentException("There are supposed to be 3 messages in "
                            + "order to set up a new course: Course name, amount of lessons and "
                            + "price. User " + user.getId() + " has sent " + m.size()
                            + " messages though.", localizationLoader.getLocalizationForUser(
                            ERROR_AMOUNT_OF_MESSAGES, user, parameterMap));
                }
                for (int i = 0; i < m.size(); i++) {
                    if (!m.get(i).hasText()) {
                        throw new InvalidDataSentException("Message " + m.get(i).getMessageId()
                                + " sent by user " + user.getId() + " does not have any text.",
                                localizationLoader.getLocalizationForUser(
                                ERROR_MESSAGE_TEXT_MISSING, user, PARAM_MESSAGE_INDEX, i));
                    }
                }
                LOGGER.debug("Prelimenary checks have been completed. "
                        + "Trying to set variables...");
                final String courseName = m.get(0).getText();

                final int amountOfLessons = Integer.parseInt(m.get(1).getText());
                if (amountOfLessons <= 0) {
                    throw new InvalidDataSentException("Course must contain at least one lesson",
                            localizationLoader.getLocalizationForUser(
                            ERROR_NEW_COURSE_WRONG_LESSONS_AMOUNT, user,
                            PARAM_AMOUNT_OF_LESSONS, amountOfLessons));
                }
                LOGGER.debug("Amount of lessons has been parsed.");

                final int price = Integer.parseInt(m.get(2).getText());
                if (price <= 0 || price > MAX_PRICE) {
                    throw new InvalidDataSentException("Price cannot be more then "
                            + MAX_PRICE + " or less than 1", localizationLoader
                            .getLocalizationForUser(ERROR_PRICE_LIMIT, user,
                            PARAM_MAX_PRICE, MAX_PRICE));
                }
                LOGGER.debug("Price has been parsed.");

                final Course course = new Course();
    
                course.setName(courseName);
                course.setPrice(price);
                course.setAmountOfLessons(amountOfLessons);
                final List<Lesson> lessons = getLessons(course);
                course.setLessons(lessons);
                course.setFeedbackIncluded(true);
                course.setHomeworkIncluded(true);
                
                LOGGER.debug("Persisting the course...");
                courseService.save(course);
                lessonService.saveAll(lessons);
                LOGGER.info("A new course " + courseName + " has been created. "
                        + "Further configuration is required to set up lessons.");
                LOGGER.debug("Sending confirmation message...");
                final Localization success = localizationLoader.getLocalizationForUser(
                        SERVICE_NEW_COURSE_CREATED, user, PARAM_COURSE_NAME, courseName);
                client.sendMessage(user, success);
                LOGGER.debug("Message sent.");
            });
            LOGGER.debug("Sending content request message...");
            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_NEW_COURSE_REQUEST, user);
            client.sendMessage(user, request);
            LOGGER.debug("Message sent.");
        }
    }

    private List<Lesson> getLessons(Course course) {
        final List<Lesson> lessons = new ArrayList<>();

        for (int i = 0; i < course.getAmountOfLessons(); i++) {
            final Lesson lesson = new Lesson();
            lesson.setCourse(course);
            lesson.setPosition(i);
            lesson.setSequenceOption(SequenceOption.BUTTON);
            lesson.setStructure(List.of());
            lessons.add(lesson);
        }
        return lessons;
    }
}
