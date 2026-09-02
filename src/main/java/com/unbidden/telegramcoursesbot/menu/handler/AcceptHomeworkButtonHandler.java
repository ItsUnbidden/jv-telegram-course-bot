package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcceptHomeworkButtonHandler extends AbstractButtonHandler {
    private static final String PROGRESS_ID_PARAM = "progressId";
    private static final String WITH_COMMENT_PARAM = "withComment";

    private final HomeworkOrchestrationService homeworkService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.GIVE_HOMEWORK_FEEDBACK)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long progressId = Long.parseLong(params.get(PROGRESS_ID_PARAM));

        if (Boolean.parseBoolean(params.get(WITH_COMMENT_PARAM))) {
            sessionService.createSession(botRole, p -> homeworkService.approve(p.botRole(),
                    progressId, p.messages()));
                    
            clientManager.sendMessage(botRole, localizationLoader.localize(
                    Localizations.Service.APPROVE_HOMEWORK_COMMENT_REQUEST, botRole));
        } else {
            homeworkService.approve(botRole, progressId);
        }
    }
}
