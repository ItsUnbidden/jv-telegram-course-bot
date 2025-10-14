package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeworkDelaySettingButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            HomeworkDelaySettingButtonHandler.class);

    private static final String PARAM_MAX_DELAY = "${maxDelay}";
    
    private static final String SERVICE_NEW_HOMEWORK_DELAY_REQUEST =
            "service_new_homework_delay_request";
    private static final String SERVICE_NEW_DELAY_SET_SUCCESS = "service_new_delay_set_success";

    private static final String ERROR_PARSE_DELAY_FAILURE = "error_parse_delay_failure";
    private static final String ERROR_HOMEWORK_DELAY_INVALID = "error_homework_delay_invalid";

    private static final int MAX_HOMEWORK_DELAY = 720;
    private static final int EXPECTED_MESSAGES = 1;

    private final HomeworkService homeworkService;

    private final ContentSessionService sessionService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Homework homework = homeworkService.getHomework(
                Long.parseLong(params[3]), user, bot);
        LOGGER.info("User " + user.getId() + " is trying to update homework "
                + homework.getId() + "'s delay...");
        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user, m, localizationLoader);
            int newDelay;
            try {
                newDelay = Integer.parseInt(m.get(0).getText());
                if (newDelay < 0) {
                    LOGGER.debug("New delay is less then zero. Setting it as 0...");
                    newDelay = 0;
                }
                if (newDelay > MAX_HOMEWORK_DELAY) {
                    throw new InvalidDataSentException("Homework delay is capped to "
                            + MAX_HOMEWORK_DELAY, localizationLoader.getLocalizationForUser(
                            ERROR_HOMEWORK_DELAY_INVALID, user, PARAM_MAX_DELAY, MAX_HOMEWORK_DELAY));
                }
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse provided string "
                        + m.get(0).getText() + " to new delay int", localizationLoader
                        .getLocalizationForUser(ERROR_PARSE_DELAY_FAILURE, user), e);
            }
            LOGGER.debug("New delay parsed successfully. Current delay: "
                    + homework.getDelay() + ".");
            homework.setDelay(newDelay);
            homeworkService.save(homework);
            LOGGER.info("Homework " + homework.getId() + "'s delay is now " + newDelay + ".");
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_NEW_DELAY_SET_SUCCESS, user));
            LOGGER.debug("Message sent.");
        }, true);
        LOGGER.debug("Sending new delay request...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_NEW_HOMEWORK_DELAY_REQUEST, user,
                PARAM_MAX_DELAY, PARAM_MAX_DELAY));
        LOGGER.debug("Request sent.");
    }
}
