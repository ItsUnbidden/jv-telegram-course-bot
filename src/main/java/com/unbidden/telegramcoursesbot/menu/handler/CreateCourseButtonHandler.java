package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateCourseButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(CreateCourseButtonHandler.class);

    private final ContentSessionService sessionService;

    private final CourseOrchestrationService courseService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        sessionService.createSession(user, bot, p -> {
            courseService.createCourse(p.user(), p.bot(), p.messages());
        });

        LOGGER.debug("Sending content request message...");
        clientManager.getClient(bot).sendMessage(user, loader.localize(
                Localizations.Service.NEW_COURSE_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}
