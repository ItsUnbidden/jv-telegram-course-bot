package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class CreateCourseButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(CreateCourseButtonHandler.class);

    private static final int NUMBER_OF_MESSAGES_EXPECTED = 3;

    private static final String SERVICE_NEW_COURSE_REQUEST = "service_new_course_request";
    private static final String SERVICE_NEW_COURSE_CREATED = "service_new_course_created";

    private static final String PARAM_COURSE_NAME = "${courseName}";

    private final ContentSessionService sessionService;

    private final CourseService courseService;

    private final LessonService lessonService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            sessionService.createSession(user, m -> {
                LOGGER.info("User " + user.getId() + " is trying to create a new course. "
                        + "Running data checks...");  
                if (m.size() != NUMBER_OF_MESSAGES_EXPECTED) {
                    throw new InvalidDataSentException("There are supposed to be 3 messages in "
                            + "order to set up a new course: Course name, amount of lessons and "
                            + "price. User " + user.getId() + " has sent " + m.size()
                            + " messages though.");
                }
                for (Message message : m) {
                    if (!message.hasText()) {
                        throw new InvalidDataSentException("Message " + message.getMessageId()
                                + " sent by user " + user.getId() + " does not have any text.");
                    }
                }
                LOGGER.debug("Prelimenary checks have been completed. "
                        + "Trying to set variables...");
                final String courseName = m.get(0).getText();

                final int amountOfLessons = Integer.parseInt(m.get(1).getText());
                if (amountOfLessons <= 0) {
                    throw new InvalidDataSentException("Course must contain at least one lesson");
                }
                LOGGER.debug("Amount of lessons has been parsed.");

                final int price = Integer.parseInt(m.get(2).getText());
                if (price <= 0) {
                    throw new InvalidDataSentException("Course price must be at least 1");
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
                bot.sendMessage(user, success);
                LOGGER.debug("Message sent.");
            });
            LOGGER.debug("Sending content request message...");
            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_NEW_COURSE_REQUEST, user);
            bot.sendMessage(user, request);
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
