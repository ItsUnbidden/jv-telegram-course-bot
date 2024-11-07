package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
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

    private static final String SERVICE_LESSON_MAPPING_ORDER_CHANGE_REQUEST =
            "service_lesson_mapping_order_change_request";
    private static final String SERVICE_LESSON_MAPPING_ORDER_CHANGE_SUCCESS =
            "service_lesson_mapping_order_change_success";

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final LessonService lessonService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Lesson lesson = lessonService.getById(Long.parseLong(params[2]));

            LOGGER.info("User " + user.getId() + " is trying to change lesson "
                    + lesson.getId() + "'s mapping order.");

            sessionService.createSession(user, m -> {
                if (m.size() != 2) {
                    throw new InvalidDataSentException("Two messages were expected but "
                            + m.size() + " was/were sent");
                }
                if (!m.get(0).hasText()) {
                        throw new InvalidDataSentException(
                                "First message is supposed to be the mapping id");
                }
                final long mappingId;
                try {
                    mappingId = Long.parseLong(m.get(0).getText());
                } catch (NumberFormatException e) {
                    throw new InvalidDataSentException("Unable to parse string "
                            + m.get(0).getText() + " to the mapping id");
                }
                if (!m.get(1).hasText()) {
                    throw new InvalidDataSentException(
                            "Second message is supposed to be the new index");
                }
                final int index;
                try {
                    index = Integer.parseInt(m.get(1).getText());
                    if (index < 0) {
                        throw new InvalidDataSentException("Index must be greater then 0");
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidDataSentException("Unable to parse string "
                            + m.get(1).getText() + " to the new index");
                }
                LOGGER.debug("User " + user.getId() + " has sent correct data. "
                        + "Changing mapping order...");
                lessonService.moveContentToIndex(lesson.getId(), mappingId, index);
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
