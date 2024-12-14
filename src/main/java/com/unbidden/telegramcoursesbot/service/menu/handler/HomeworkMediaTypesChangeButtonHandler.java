package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeworkMediaTypesChangeButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            HomeworkMediaTypesChangeButtonHandler.class);

    private static final String PARAM_MEDIA_TYPES = "${mediaTypes}";
    
    private static final String SERVICE_MEDIA_TYPES_REQUEST =
            "service_homework_media_types_request";
    private static final String SERVICE_MEDIA_TYPES_UPDATE_SUCCESS =
            "service_homework_media_types_update_success";

    private static final String ERROR_PARSE_ENUM_FAILURE = "error_parse_enum_failure";

    private static final String MEDIA_TYPE_DIVIDER = " ";
    private static final int EXPECTED_MESSAGES = 1;

    private final ContentSessionService sessionService;

    private final HomeworkService homeworkService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Homework homework = homeworkService.getHomework(
                Long.parseLong(params[3]), user, bot);
        LOGGER.info("User " + user.getId() + " is trying to update allowed media "
                + "types for homework " + homework.getId() + ".");
        
        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user, m, localizationLoader);

            final String mediaTypesStr = m.get(0).getText().trim();
            final String[] potentialMediaTypes = mediaTypesStr.split(MEDIA_TYPE_DIVIDER);
            LOGGER.debug("User " + user.getId() + " has provided this string: "
                    + mediaTypesStr + ". Trying to parse...");    
            for (String mediaTypeStr : potentialMediaTypes) {
                try {
                    LOGGER.debug("Trying to parse " + mediaTypeStr + " to "
                            + MediaType.class.getName() + "...");
                    final MediaType value = MediaType.valueOf(mediaTypeStr);
                    LOGGER.debug("Parsed to " + value + ".");
                } catch (IllegalArgumentException e) {
                    throw new InvalidDataSentException("Unable to parse " + mediaTypeStr
                            + " to a " + MediaType.class.getName() + " enum value",
                            localizationLoader.getLocalizationForUser(
                            ERROR_PARSE_ENUM_FAILURE, user, PARAM_MEDIA_TYPES,
                            Arrays.toString(MediaType.values())), e);
                }
            }
            LOGGER.debug("No problems occured during parsing. Persisting...");
            homework.setAllowedMediaTypes(mediaTypesStr);
            homeworkService.save(homework);
            LOGGER.info("Media types updated to " + mediaTypesStr + " for homework "
                    + homework.getId() + ".");
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_MEDIA_TYPES_UPDATE_SUCCESS, user,
                    PARAM_MEDIA_TYPES, mediaTypesStr));
            LOGGER.debug("Message sent.");
        }, true);
        LOGGER.debug("Sending media types request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_MEDIA_TYPES_REQUEST, user, PARAM_MEDIA_TYPES,
                homework.getAllowedMediaTypes()));
        LOGGER.debug("Message sent.");
    }
}
