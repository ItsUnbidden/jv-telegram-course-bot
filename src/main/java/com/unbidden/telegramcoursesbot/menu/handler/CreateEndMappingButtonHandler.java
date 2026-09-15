package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class CreateEndMappingButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final ContentSessionService sessionService;

    private final CourseOrchestrationService courseService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));

        sessionService.createSession(botRole, p -> {
            courseService.addEndMapping(botRole, courseId, p.messages());
        });

        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.NEW_END_MAPPING_REQUEST, botRole));
    }
}
