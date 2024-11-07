package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
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
import org.telegram.telegrambots.meta.api.objects.Message;

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

    private final LocalizationLoader localizationLoader;

    private final ContentSessionService sessionService;

    private final LessonService lessonService;

    private final ContentService contentService;

    private final UserService userService;

    private final TelegramBot bot;
    
    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            LOGGER.debug("Attempting to parse lesson id sent by user " + user.getId() + "...");
            final Long lessonId = Long.parseLong(params[2]);
            LOGGER.debug("Lesson id is " + lessonId + ". Creating session for adding content...");
            sessionService.createSession(user, m -> {
                LOGGER.debug("Adding content to lesson " + lessonId + "...");
                
                final Lesson lesson = lessonService.getById(lessonId);
                final String localizationName = "course_" + lesson.getCourse().getName()
                        + "_lesson_" + lesson.getPosition() + "_content_"
                        + lesson.getStructure().size();
                final Message lastMessage = m.get(m.size() - 1);
                final String languageCode;

                if (lastMessage.hasText()) {
                    if (lastMessage.getText().length() > 3
                            || lastMessage.getText().length() < 2) {
                        throw new InvalidDataSentException("Language code must be "
                                + "between 2 and 3 characters");
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
                final LocalizedContent content = contentService.parseAndPersistContent(m,
                        localizationName, languageCode);
                lessonService.addContent(lessonId, content);
                LOGGER.debug("Content " + content.getId() + " has been added. Sending "
                        + "confirmation message...");
    
                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_LESSON_ID, lessonId);
                parameterMap.put(PARAM_CONTENT_ID, content.getId());
                final Localization success = localizationLoader.getLocalizationForUser(
                        SERVICE_LESSON_CONTENT_ADDED, user, parameterMap);
                bot.sendMessage(user, success);
                LOGGER.debug("Message sent.");
            });
            LOGGER.debug("Sending content request message...");
            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_ADD_LESSON_CONTENT_REQUEST, user, PARAM_LESSON_ID, lessonId);
            bot.sendMessage(user, request);
            LOGGER.debug("Message sent.");
        }
    }
}
