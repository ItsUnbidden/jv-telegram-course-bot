package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.MoveContentException;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateContentPositionButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            UpdateContentPositionButtonHandler.class);

    private static final String PARAM_INDEX = "${index}";
    private static final String PARAM_MAPPING_ID = "${mappingId}";
    private static final String PARAM_LESSON_ID = "${lessonId}";
    private static final String PARAM_PROVIDED_MESSAGES_AMOUNT = "${providedMessagesNumber}";
    private static final String PARAM_EXPECTED_MESSAGES_AMOUNT = "${expectedMessagesAmount}";
    private static final String PARAM_MAX_VALUE = "${maxValue}";
    private static final String PARAM_MESSAGE_INDEX = "${messageIndex}";

    private static final String SERVICE_LESSON_MAPPING_ORDER_CHANGE_REQUEST =
            "service_lesson_mapping_order_change_request";
    private static final String SERVICE_LESSON_MAPPING_ORDER_CHANGE_SUCCESS =
            "service_lesson_mapping_order_change_success";
        
    private static final String ERROR_AMOUNT_OF_MESSAGES = "error_amount_of_messages";
    private static final String ERROR_INDEX_LIMIT = "error_index_limit";
    private static final String ERROR_PARSE_INDEX_FAILURE = "error_parse_index_failure";
    private static final String ERROR_MESSAGE_TEXT_MISSING = "error_message_text_missing";
    private static final String ERROR_PARSE_ID_FAILURE = "error_parse_id_failure";
    private static final String ERROR_SAME_CONTENT_POSITION = "error_same_content_position";

    private static final int NUMBER_OF_MESSAGES_EXPECTED = 2;

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final LessonService lessonService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Lesson lesson = lessonService.getById(Long.parseLong(params[2]), user);

            LOGGER.info("User " + user.getId() + " is trying to change lesson "
                    + lesson.getId() + "'s mapping order.");

            sessionService.createSession(user, m -> {
                if (m.size() != NUMBER_OF_MESSAGES_EXPECTED) {
                    final Map<String, Object> parameterMap = new HashMap<>();
                    parameterMap.put(PARAM_EXPECTED_MESSAGES_AMOUNT, NUMBER_OF_MESSAGES_EXPECTED);
                    parameterMap.put(PARAM_PROVIDED_MESSAGES_AMOUNT, m.size());

                    throw new InvalidDataSentException("Two messages were expected but "
                            + m.size() + " was/were sent", localizationLoader
                            .getLocalizationForUser(ERROR_AMOUNT_OF_MESSAGES, user,
                            parameterMap));
                }
                if (!m.get(0).hasText()) {
                        throw new InvalidDataSentException("First message is supposed to be "
                                + "the mapping id", localizationLoader.getLocalizationForUser(
                                    ERROR_MESSAGE_TEXT_MISSING, user, PARAM_MESSAGE_INDEX, 0));
                }
                final long mappingId;
                try {
                    mappingId = Long.parseLong(m.get(0).getText());
                } catch (NumberFormatException e) {
                    throw new InvalidDataSentException("Unable to parse string "
                            + m.get(0).getText() + " to the mapping id", localizationLoader
                            .getLocalizationForUser(ERROR_PARSE_ID_FAILURE, user));
                }
                if (!m.get(1).hasText()) {
                    throw new InvalidDataSentException(
                            "Second message is supposed to be the new index",
                            localizationLoader.getLocalizationForUser(ERROR_MESSAGE_TEXT_MISSING,
                            user, PARAM_MESSAGE_INDEX, 1));
                }
                final int index;
                try {
                    index = Integer.parseInt(m.get(1).getText());
                    if (index < 0 || index >= lesson.getStructure().size()) {
                        throw new InvalidDataSentException("Index must be greater then 0",
                        localizationLoader.getLocalizationForUser(ERROR_INDEX_LIMIT, user,
                        PARAM_MAX_VALUE, lesson.getStructure().size()));
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidDataSentException("Unable to parse string "
                            + m.get(1).getText() + " to the new index", localizationLoader
                            .getLocalizationForUser(ERROR_PARSE_INDEX_FAILURE, user));
                }
                LOGGER.debug("User " + user.getId() + " has sent correct data. "
                        + "Changing mapping order...");
                try {
                    lessonService.moveContentToIndex(lesson.getId(), mappingId, index, user);
                } catch (MoveContentException e) {
                    throw new InvalidDataSentException("Sent index is already applied",
                            localizationLoader.getLocalizationForUser(
                            ERROR_SAME_CONTENT_POSITION, user));
                }
                LOGGER.info("Mapping order for lesson " + lesson.getId()
                        + "'s content has been changed.");
                LOGGER.debug("Sending confirmation message...");

                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_MAPPING_ID, mappingId);
                parameterMap.put(PARAM_INDEX, index);
                
                bot.sendMessage(user, localizationLoader.getLocalizationForUser(
                        SERVICE_LESSON_MAPPING_ORDER_CHANGE_SUCCESS, user));
                LOGGER.debug("Message sent.");
            });
            LOGGER.debug("Sending mapping order change request...");
            bot.sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_LESSON_MAPPING_ORDER_CHANGE_REQUEST, user,
                    PARAM_LESSON_ID, lesson.getId()));
            LOGGER.debug("Message sent.");
        }
    }
}
