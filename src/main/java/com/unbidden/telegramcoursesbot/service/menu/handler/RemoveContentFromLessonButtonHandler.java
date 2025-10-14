package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
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
public class RemoveContentFromLessonButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager
            .getLogger(AddContentToLessonButtonHandler.class);
    
    private static final String PARAM_CONTENT_ID = "${contentId}";
    private static final String PARAM_LESSON_ID = "${lessonId}";

    private static final String SERVICE_REMOVE_LESSON_CONTENT_REQUEST =
            "service_remove_lesson_content_request";
    private static final String SERVICE_LESSON_CONTENT_REMOVED =
            "service_lesson_content_removed";
    
    private static final String ERROR_PARSE_CONTENT_ID_FAILURE = "error_parse_content_id_failure";

    private static final int EXPECTED_MESSAGES = 1;

    private final LocalizationLoader localizationLoader;

    private final ContentSessionService sessionService;

    private final TextUtil textUtil;

    private final LessonService lessonService;

    private final ClientManager clientManager;
    
    @Override
    @Security(authorities =  AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        LOGGER.debug("Attempting to parse lesson id sent by user " + user.getId() + "...");
        final Long lessonId = Long.parseLong(params[2]);
        LOGGER.debug("Lesson id is " + lessonId
                + ". Creating session for removing content...");
        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user, m, localizationLoader);
            LOGGER.debug("Removing content from lesson " + lessonId + "...");
            try {
                final Long contentId = Long.parseLong(m.get(0).getText());
                LOGGER.debug("Content id is " + contentId + ".");
                lessonService.removeContent(lessonId, contentId, user, bot);
                LOGGER.info("Content " + contentId + " has been removed from lesson "
                        + lessonId + ".");

                LOGGER.debug( "Sending confirmation message...");
                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_LESSON_ID, lessonId);
                parameterMap.put(PARAM_CONTENT_ID, contentId);
                final Localization success = localizationLoader.getLocalizationForUser(
                        SERVICE_LESSON_CONTENT_REMOVED, user, parameterMap);
                clientManager.getClient(bot).sendMessage(user, success);
                LOGGER.debug("Message sent.");
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse provided string "
                        + m.get(0).getText() + " to new price int", localizationLoader
                        .getLocalizationForUser(ERROR_PARSE_CONTENT_ID_FAILURE, user), e);
            }
        }, true);
        LOGGER.debug("Sending content request message...");
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_REMOVE_LESSON_CONTENT_REQUEST, user);
        clientManager.getClient(bot).sendMessage(user, request);
        LOGGER.debug("Message sent.");
    }
}
