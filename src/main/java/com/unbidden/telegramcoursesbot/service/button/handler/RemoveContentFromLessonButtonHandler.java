package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
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
public class RemoveContentFromLessonButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager
            .getLogger(AddContentToLessonButtonHandler.class);
    
    private static final String PARAM_CONTENT_ID = "${contentId}";
    private static final String PARAM_LESSON_ID = "${lessonId}";

    private static final String SERVICE_REMOVE_LESSON_CONTENT_REQUEST =
            "service_remove_lesson_content_request";
    private static final String SERVICE_LESSON_CONTENT_REMOVED =
            "service_lesson_content_removed";
    
    private static final String ERROR_TEXT_MESSAGE_EXPECTED = "error_text_message_expected";

    private final LocalizationLoader localizationLoader;

    private final ContentSessionService sessionService;

    private final LessonService lessonService;

    private final UserService userService;

    private final TelegramBot bot;
    
    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            LOGGER.debug("Attempting to parse lesson id sent by user " + user.getId() + "...");
            final Long lessonId = Long.parseLong(params[2]);
            LOGGER.debug("Lesson id is " + lessonId
                    + ". Creating session for removing content...");
            sessionService.createSession(user, m -> {
                LOGGER.debug("Removing content from lesson " + lessonId + "...");
                if (!m.get(0).hasText()) {
                    throw new InvalidDataSentException("Content id must be provided",
                    localizationLoader.getLocalizationForUser(ERROR_TEXT_MESSAGE_EXPECTED, user));
                }
                final Long contentId = Long.parseLong(m.get(0).getText());
                LOGGER.debug("Content id is " + contentId + ".");
                lessonService.removeContent(lessonId, contentId, user);
                LOGGER.info("Content " + contentId + " has been removed from lesson "
                        + lessonId + ".");

                LOGGER.debug( "Sending confirmation message...");
                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_LESSON_ID, lessonId);
                parameterMap.put(PARAM_CONTENT_ID, contentId);
                final Localization success = localizationLoader.getLocalizationForUser(
                        SERVICE_LESSON_CONTENT_REMOVED, user, parameterMap);
                bot.sendMessage(user, success);
                LOGGER.debug("Message sent.");
            }, true);
            LOGGER.debug("Sending content request message...");
            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_REMOVE_LESSON_CONTENT_REQUEST, user);
            bot.sendMessage(user, request);
            LOGGER.debug("Message sent.");
        }
    }
}
