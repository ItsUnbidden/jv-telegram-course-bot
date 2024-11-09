package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateCourseRefundStageButtonHandler implements ButtonHandler {
    private static final String ERROR_SAME_NEW_REFUND_STAGE = "error_same_new_refund_stage";

    private static final Logger LOGGER = LogManager
            .getLogger(UpdateCourseRefundStageButtonHandler.class);

    private static final String PARAM_NEW_REFUND_STAGE = "${newRefundStage}";
    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_AMOUNT_OF_LESSONS = "${amountOfLessons}";
    private static final String PARAM_MESSAGE_INDEX = "${messageIndex}";

    private static final String SERVICE_NEW_REFUND_STAGE_REQUEST =
            "service_new_refund_stage_request";
    private static final String SERVICE_NEW_REFUND_STAGE_SUCCESS =
            "service_new_refund_stage_success";

    private static final String ERROR_TEXT_MESSAGE_EXPECTED = "error_text_message_expected";
    private static final String ERROR_PARSE_REFUND_STAGE_FAILURE =
            "error_parse_refund_stage_failure";
    private static final String ERROR_REFUND_STAGE_BIGGER_THEN_AMOUNT_OF_LESSONS =
            "error_refund_stage_bigger_then_amount_of_lessons";

    private final ContentSessionService sessionService;

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        final Course course = courseService.getCourseByName(params[0], user);

        LOGGER.info("User " + user.getId() + " is trying to change refund stage in course "
                + course.getRefundStage() + "...");
        sessionService.createSession(user, m -> {
            if (!m.get(0).hasText()) {
                throw new InvalidDataSentException("One text message was expected",
                        localizationLoader.getLocalizationForUser(ERROR_TEXT_MESSAGE_EXPECTED,
                        user, PARAM_MESSAGE_INDEX, 0));
            }
            try {
                LOGGER.debug("Trying to parse " + m.get(0).getText() + " new refund stage...");
                Integer newRefundStage = Integer.parseInt(m.get(0).getText());
                LOGGER.debug("New refund stage parsed.");
                if (newRefundStage < -1) {
                    LOGGER.debug("Number is less then -1. It will be forced to -1.");
                    newRefundStage = -1;
                }
                if (newRefundStage > course.getAmountOfLessons() - 1) {
                    throw new InvalidDataSentException("Specified new refund stage is bigger "
                            + "then the amount of lessons for course " + course.getName(),
                            localizationLoader.getLocalizationForUser(
                            ERROR_REFUND_STAGE_BIGGER_THEN_AMOUNT_OF_LESSONS, user,
                            PARAM_AMOUNT_OF_LESSONS, course.getAmountOfLessons()));
                }
                if (newRefundStage.equals(course.getRefundStage())) {
                    throw new InvalidDataSentException("New refund stage is the same as before",
                            localizationLoader.getLocalizationForUser(
                            ERROR_SAME_NEW_REFUND_STAGE, user));
                }
                LOGGER.debug("New refund stage seems ligit. Setting...");
                course.setRefundStage(newRefundStage);
                courseService.save(course);
                LOGGER.info("New refund stage of " + newRefundStage + " has been set and "
                        + "persisted for course " + course.getName() + ".");
                LOGGER.debug("Sending confirmation message...");
                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_COURSE_NAME, course.getName());
                parameterMap.put(PARAM_NEW_REFUND_STAGE, course.getName());

                client.sendMessage(user, localizationLoader.getLocalizationForUser(
                        SERVICE_NEW_REFUND_STAGE_SUCCESS, user, parameterMap));
                LOGGER.debug("Message sent.");
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse string "
                        + m.get(0).getText() + " to new refund stage", localizationLoader
                        .getLocalizationForUser(ERROR_PARSE_REFUND_STAGE_FAILURE, user));
            }
        }, true);
        LOGGER.debug("Sending new refund stage message request...");
        client.sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_NEW_REFUND_STAGE_REQUEST, user, PARAM_COURSE_NAME, course.getName()));
        LOGGER.debug("Message sent.");
    }
}
