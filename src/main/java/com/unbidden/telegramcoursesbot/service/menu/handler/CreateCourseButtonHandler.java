package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.regex.Pattern;
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
    private static final String PARAM_AMOUNT_OF_LESSONS = "${amountOfLessons}";

    private static final String SERVICE_NEW_COURSE_REQUEST = "service_new_course_request";
    private static final String SERVICE_NEW_COURSE_CREATED = "service_new_course_created";
    
    private static final String ERROR_PRICE_LIMIT = "error_price_limit";
    private static final String ERROR_NEW_COURSE_WRONG_LESSONS_AMOUNT =
            "error_new_course_wrong_lessons_amount";
    private static final String ERROR_PARSE_AMOUNT_OF_LESSONS = "error_parse_amount_of_lessons";
    private static final String ERROR_PARSE_PRICE_FAILURE = "error_parse_price_failure";
    private static final String ERROR_COURSE_ALREADY_EXISTS = "error_course_already_exists";
    private static final String ERROR_COURSE_NAME_PATTERN_MISMATCH =
            "error_course_name_pattern_mismatch";
    private static final String ERROR_COURSE_NAME_LENGTH = "error_course_name_length";
    
    private static final int MAX_PRICE = 100_000;
    private static final int MIN_COURSE_NAME_LENGTH = 3;
    private static final int MAX_COURSE_NAME_LENGTH = 20;
    private static final Pattern COURSE_NAME_PATTERN = Pattern
            .compile("[a-z0-9_]+");

    private static final int NUMBER_OF_MESSAGES_EXPECTED = 3;

    private final ContentSessionService sessionService;

    private final CourseService courseService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        sessionService.createSession(user, bot, m -> {
            LOGGER.info("User " + user.getId() + " is trying to create a new course. "
                    + "Running data checks...");  
            textUtil.checkExpectedMessages(NUMBER_OF_MESSAGES_EXPECTED, user,
                    m, localizationLoader);
            final String courseName = m.get(0).getText();
            if (courseName.length() < MIN_COURSE_NAME_LENGTH
                    || courseName.length() > MAX_COURSE_NAME_LENGTH) {
                throw new InvalidDataSentException("Course name length cannot be shorter "
                        + "than " + MIN_COURSE_NAME_LENGTH + " and longer than "
                        + MAX_COURSE_NAME_LENGTH, localizationLoader.getLocalizationForUser(
                        ERROR_COURSE_NAME_LENGTH, user));
            }
            if (!COURSE_NAME_PATTERN.matcher(courseName).matches()) {
                throw new InvalidDataSentException("Course name " + courseName
                        + " does not match the course name pattern", localizationLoader
                        .getLocalizationForUser(ERROR_COURSE_NAME_PATTERN_MISMATCH, user));
            }
            try {
                final Course course = courseService.getCourseByName(courseName, user, bot);
                throw new InvalidDataSentException("Course " + courseName + " already exists "
                        + "under id " + course.getId(), localizationLoader.getLocalizationForUser(
                        ERROR_COURSE_ALREADY_EXISTS, user));
            } catch (EntityNotFoundException e) {
                LOGGER.debug("Course " + courseName + " does not exist. Proceeding...");
            }

            final int amountOfLessons;
            try {
                amountOfLessons = Integer.parseInt(m.get(1).getText());
                if (amountOfLessons <= 0) {
                    throw new InvalidDataSentException("Course must contain at least one lesson",
                            localizationLoader.getLocalizationForUser(
                            ERROR_NEW_COURSE_WRONG_LESSONS_AMOUNT, user,
                            PARAM_AMOUNT_OF_LESSONS, amountOfLessons));
                }
                LOGGER.debug("Amount of lessons has been parsed.");
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse provided string "
                        + m.get(1).getText() + " to amount of lessons int", localizationLoader
                        .getLocalizationForUser(ERROR_PARSE_AMOUNT_OF_LESSONS, user), e);
            }

            final int price;
            try {
                price = Integer.parseInt(m.get(2).getText());
                if (price <= 0 || price > MAX_PRICE) {
                    throw new InvalidDataSentException("Price cannot be more then "
                            + MAX_PRICE + " or less than 1", localizationLoader
                            .getLocalizationForUser(ERROR_PRICE_LIMIT, user,
                            PARAM_MAX_PRICE, MAX_PRICE));
                }
                LOGGER.debug("Price has been parsed.");
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse provided string "
                        + m.get(2).getText() + " to new price int", localizationLoader
                        .getLocalizationForUser(ERROR_PARSE_PRICE_FAILURE, user), e);
            }

            courseService.createCourse(bot, courseName, price, amountOfLessons);
            LOGGER.info("A new course " + courseName + " has been created. "
                    + "Further configuration is required to set up lessons.");
            LOGGER.debug("Sending confirmation message...");
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_NEW_COURSE_CREATED, user, PARAM_COURSE_NAME, courseName);
            clientManager.getClient(bot).sendMessage(user, success);
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending content request message...");
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_NEW_COURSE_REQUEST, user);
        clientManager.getClient(bot).sendMessage(user, request);
        LOGGER.debug("Message sent.");
    }
}
