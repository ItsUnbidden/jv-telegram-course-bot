package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.security.SecurityService;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {
    private static final Logger LOGGER = LogManager.getLogger(StartCommandHandler.class);

    private static final String COMMAND = "/start";

    private final LocalizationLoader localizationLoader;

    private final CourseOrchestrationService courseService;

    private final ContentService contentService;

    private final SecurityService securityService;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.INFO)
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        LOGGER.info("User " + user.getId() + " triggered the /start command.");
        if (bot.getStart() == null) {
            LOGGER.info("There is no custom start message for bot " + bot.getId() + ". A default localization will be sent.");
            clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                    Localizations.Service.NO_START, user));
        } else {
            LOGGER.debug("Sending /start message to user " + user.getId() + "...");
            contentService.sendLocalizedContent(user, bot, bot.getStart().getId());
        }
        LOGGER.debug("Message sent.");

        if (commandParts.length > 2) {
            LOGGER.debug("Additional command parameters present: "
                    + Arrays.toString(commandParts) + ".");

            switch (commandParts[1]) {
                case "course" -> {
                    if (securityService.grantAccess(user, bot, AuthorityType.LAUNCH_COURSE,
                            AuthorityType.BUY)) {
                        try {
                            courseService.initCourse(user, bot, Long.parseLong(commandParts[2]));
                        } catch (NumberFormatException e) {
                            throw new InvalidDataSentException("Failed to parse course id in a command sent by user "
                                    + user.getId() + ". Supplied value: " + commandParts[2], localizationLoader
                                    .getLocalizationForUser(Localizations.Error.PARSE_ID_FAILURE, user));
                        }
                    }
                }
                case "payment" -> {
                    // TODO: currently not implemented
                }
                default -> {
                    throw new InvalidDataSentException("Unknown command parameter was sent: "
                            + commandParts[1] + " by user " + user.getId() + ".", localizationLoader
                            .getLocalizationForUser(Localizations.Error.INVALID_START_PARAM, user));
                }
            }
        }
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.INFO);
    }
}
