package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeworkMediaTypesChangeButtonHandler extends AbstractButtonHandler {
    private static final String HOMEWORK_ID_PARAM = "homeworkId";

    private final ContentSessionService sessionService;

    private final HomeworkOrchestrationService homeworkService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long homeworkId = Long.parseLong(params.get(HOMEWORK_ID_PARAM));

        sessionService.createSession(botRole, p -> {
            homeworkService.updateAllowedMediaTypes(p.botRole(), homeworkId, p.messages());
        }, true);

        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.MEDIA_TYPES_REQUEST, botRole));
    }
}
