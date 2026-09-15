package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.security.SecurityService;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
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

    private final ContentOrchestrationService contentService;

    private final SecurityService securityService;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.INFO)
    public void handle(BotRole botRole, Message message, String[] commandParts) {
        LOGGER.info("User " + botRole.getUser().getId() + " triggered the /start command.");
        if (botRole.getBot().getStart() == null) {
            LOGGER.info("There is no custom start message for bot " + botRole.getBot().getId() + ". A default localization will be sent.");
            clientManager.sendMessage(botRole, localizationLoader.localize(
                    Localizations.Service.NO_START, botRole));
        } else {
            LOGGER.debug("Sending /start message to user " + botRole.getUser().getId() + "...");
            contentService.sendLocalizedContent(botRole, botRole.getBot().getStart().getId());
        }
        LOGGER.debug("Message sent.");

        if (commandParts.length > 2) {
            LOGGER.debug("Additional command parameters present: "
                    + Arrays.toString(commandParts) + ".");

            switch (commandParts[1]) {
                case "course" -> {
                    if (securityService.grantAccess(botRole, AuthorityType.LAUNCH_COURSE, AuthorityType.BUY)) {
                        try {
                            courseService.initCourse(botRole, Long.parseLong(commandParts[2]));
                        } catch (NumberFormatException e) {
                            throw new InvalidDataSentException("Failed to parse course id in a command sent by user "
                                    + botRole.getUser().getId() + ". Supplied value: " + commandParts[2], localizationLoader
                                    .localize(Localizations.Error.PARSE_ID_FAILURE, botRole));
                        }
                    }
                }
                case "payment" -> {
                    // TODO: currently not implemented
                }
                default -> {
                    throw new InvalidDataSentException("Unknown command parameter was sent: "
                            + commandParts[1] + " by user " + botRole.getUser().getId() + ".", localizationLoader
                            .localize(Localizations.Error.INVALID_START_PARAM, botRole));
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
