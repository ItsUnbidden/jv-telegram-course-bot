package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.MoveContentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.TextUtil;

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
    private static final String PARAM_MAX_VALUE = "${maxValue}";

    private static final String SERVICE_LESSON_MAPPING_ORDER_CHANGE_REQUEST =
            "service_lesson_mapping_order_change_request";
    private static final String SERVICE_LESSON_MAPPING_ORDER_CHANGE_SUCCESS =
            "service_lesson_mapping_order_change_success";
        
    private static final String ERROR_INDEX_LIMIT = "error_index_limit";
    private static final String ERROR_PARSE_INDEX_FAILURE = "error_parse_index_failure";
    private static final String ERROR_PARSE_ID_FAILURE = "error_parse_id_failure";
    private static final String ERROR_SAME_CONTENT_POSITION = "error_same_content_position";

    private static final int NUMBER_OF_MESSAGES_EXPECTED = 2;

    private final ContentSessionService sessionService;

    private final LessonService lessonService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Lesson lesson = lessonService.getById(Long.parseLong(params[2]), user, bot);

        LOGGER.info("User " + user.getId() + " is trying to change lesson "
                + lesson.getId() + "'s mapping order.");

        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(NUMBER_OF_MESSAGES_EXPECTED, user,
                    m, localizationLoader);
            final long mappingId;
            try {
                mappingId = Long.parseLong(m.get(0).getText());
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse string "
                        + m.get(0).getText() + " to the mapping id", localizationLoader
                        .getLocalizationForUser(ERROR_PARSE_ID_FAILURE, user));
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
                lessonService.moveContentToIndex(lesson.getId(), mappingId, index, user, bot);
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
            
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_LESSON_MAPPING_ORDER_CHANGE_SUCCESS,
                    user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending mapping order change request...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_LESSON_MAPPING_ORDER_CHANGE_REQUEST, user,
                PARAM_LESSON_ID, lesson.getId()));
        LOGGER.debug("Message sent.");
    }
}
