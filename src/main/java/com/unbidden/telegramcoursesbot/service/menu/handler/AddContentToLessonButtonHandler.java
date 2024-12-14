package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class AddContentToLessonButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager
            .getLogger(AddContentToLessonButtonHandler.class);
    
    private static final String PARAM_CONTENT_ID = "${contentId}";
    private static final String PARAM_LESSON_ID = "${lessonId}";

    private static final String SERVICE_ADD_LESSON_CONTENT_REQUEST =
            "service_add_lesson_content_request";
    private static final String SERVICE_LESSON_CONTENT_ADDED = "service_lesson_content_added";

    private static final String ERROR_LANGUAGE_CODE_LENGTH = "error_language_code_length";

    private static final String COURSE_LESSON_CONTENT = "course_%s_lesson_%s_content_%s";

    private final LocalizationLoader localizationLoader;

    private final ContentSessionService sessionService;

    private final LessonService lessonService;

    private final ContentService contentService;

    private final ClientManager clientManager;
    
    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        LOGGER.debug("Attempting to parse lesson id sent by user " + user.getId() + "...");
        final Long lessonId = Long.parseLong(params[2]);
        LOGGER.debug("Lesson id is " + lessonId + ". Creating session for adding content...");
        sessionService.createSession(user, bot, m -> {
            LOGGER.debug("Adding content to lesson " + lessonId + "...");
            
            final Lesson lesson = lessonService.getById(lessonId, user, bot);
            final String localizationName = COURSE_LESSON_CONTENT.formatted(
                    lesson.getCourse().getName(), lesson.getPosition(),
                    lesson.getStructure().size());
            final Message lastMessage = m.get(m.size() - 1);
            final String languageCode;

            if (lastMessage.hasText()) {
                if (lastMessage.getText().length() > 3
                        || lastMessage.getText().length() < 2) {
                    throw new InvalidDataSentException("Language code must be "
                            + "between 2 and 3 characters", localizationLoader
                            .getLocalizationForUser(ERROR_LANGUAGE_CODE_LENGTH, user));
                }
                LOGGER.debug("Language code for new content will be "
                        + lastMessage.getText() + ".");
                languageCode = lastMessage.getText();
            } else {
                languageCode = user.getLanguageCode();
                LOGGER.debug("Seems like language code is not specified. "
                        + "Assuming it to be user's telegram language which is "
                        + languageCode + ".");
            }
            final LocalizedContent content = contentService.parseAndPersistContent(bot, m,
                    localizationName, languageCode);
            lessonService.addContent(lessonId, content, user, bot);
            LOGGER.debug("Content " + content.getId() + " has been added. Sending "
                    + "confirmation message...");

            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_LESSON_ID, lessonId);
            parameterMap.put(PARAM_CONTENT_ID, content.getId());
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_LESSON_CONTENT_ADDED, user, parameterMap);
            clientManager.getClient(bot).sendMessage(user, success);
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending content request message...");
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_ADD_LESSON_CONTENT_REQUEST, user, PARAM_LESSON_ID, lessonId);
        clientManager.getClient(bot).sendMessage(user, request);
        LOGGER.debug("Message sent.");
    }
}
