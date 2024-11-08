package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
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
public class UpdateHomeworkContentButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager
            .getLogger(UpdateHomeworkContentButtonHandler.class);
    
    private static final String PARAM_MAPPING_ID = "${mappingId}";
    private static final String PARAM_HOMEWORK_ID = "${homeworkId}";
    private static final String PARAM_LESSON_ID = "${lessonId}";

    private static final String SERVICE_HOMEWORK_CONTENT_REQUEST =
            "service_homework_content_request";
    private static final String SERVICE_HOMEWORK_CONTENT_UPDATED =
            "service_homework_content_updated";

    private static final String ERROR_LANGUAGE_CODE_LENGTH = "error_language_code_length";

    private final ContentSessionService sessionService;

    private final HomeworkService homeworkService;

    private final UserService userService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Homework homework = homeworkService.getHomework(
                    Long.parseLong(params[3]), user);

            sessionService.createSession(user, m -> {
                LOGGER.info("User " + user.getId() + " is trying to update homework "
                        + homework.getId() + "...");  
                final String localizationName = homework.getMapping().getContent().get(0)
                        .getData().getData();
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
                final LocalizedContent content = contentService.parseAndPersistContent(
                        m, localizationName, languageCode);
                final ContentMapping newMapping = homeworkService
                        .updateContent(homework.getId(), content, user);
                LOGGER.info("Homework " + homework.getId() + " content has been updated.");
                LOGGER.debug("Sending confirmation message...");

                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_HOMEWORK_ID, homework.getId());
                parameterMap.put(PARAM_MAPPING_ID, newMapping.getId());
 
                final Localization success = localizationLoader.getLocalizationForUser(
                        SERVICE_HOMEWORK_CONTENT_UPDATED, user, parameterMap);
                bot.sendMessage(user, success);
                LOGGER.debug("Message sent.");
            });
            LOGGER.debug("Sending content request message...");
            final Localization request = localizationLoader.getLocalizationForUser(
                    SERVICE_HOMEWORK_CONTENT_REQUEST, user, PARAM_LESSON_ID,
                    homework.getLesson().getId());
            bot.sendMessage(user, request);
            LOGGER.debug("Message sent.");
        }
    }
}
