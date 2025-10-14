package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
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

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Homework homework = homeworkService.getHomework(
                Long.parseLong(params[3]), user, bot);

        sessionService.createSession(user, bot, m -> {
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
            final LocalizedContent content = contentService.parseAndPersistContent(bot,
                    m, localizationName, languageCode);
            final ContentMapping newMapping = homeworkService
                    .updateContent(homework.getId(), content, user, bot);
            LOGGER.info("Homework " + homework.getId() + " content has been updated.");
            LOGGER.debug("Sending confirmation message...");

            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_HOMEWORK_ID, homework.getId());
            parameterMap.put(PARAM_MAPPING_ID, newMapping.getId());

            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_HOMEWORK_CONTENT_UPDATED, user, parameterMap);
            clientManager.getClient(bot).sendMessage(user, success);
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending content request message...");
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_HOMEWORK_CONTENT_REQUEST, user, PARAM_LESSON_ID,
                homework.getLesson().getId());
        clientManager.getClient(bot).sendMessage(user, request);
        LOGGER.debug("Message sent.");
    }
}
