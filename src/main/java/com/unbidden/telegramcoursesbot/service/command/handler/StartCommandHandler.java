package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.security.SecurityService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {
    private static final String COMMAND = "/start";

    private static final String SERVICE_START = "service_%s_start";

    private static final Logger LOGGER = LogManager.getLogger(StartCommandHandler.class);

    private final LocalizationLoader localizationLoader;

    private final CourseService courseService;

    private final SecurityService securityService;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.INFO)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Message message,
            @NonNull String[] commandParts) {
        LOGGER.info("Sending /start message to user " + user.getId() + "...");
        final Localization localization = localizationLoader.getLocalizationForUser(
            SERVICE_START.formatted(bot.getName()), user);
        clientManager.getClient(bot).sendMessage(user, localization);
        LOGGER.info("Message sent.");

        if (commandParts.length > 1) {
            LOGGER.info("Additional command parameters present: "
                    + Arrays.toString(commandParts) + ".");
            try {
                if (securityService.grantAccess(bot, user, AuthorityType.LAUNCH_COURSE,
                        AuthorityType.BUY)) {
                    courseService.initMessage(user, bot, commandParts[0]);
                }
            } catch (EntityNotFoundException e) {
                LOGGER.warn("Additional parameters sent by user " + user.getId()
                        + " are invalid and will be ignored.");
            }
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    @NonNull
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.INFO);
    }
}
