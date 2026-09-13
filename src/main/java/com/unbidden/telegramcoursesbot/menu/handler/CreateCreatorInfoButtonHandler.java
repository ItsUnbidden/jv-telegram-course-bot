package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.bot.BotOrchestrationService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class CreateCreatorInfoButtonHandler extends AbstractButtonHandler {
    private final BotOrchestrationService botService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    public void handle(BotRole botRole, Map<String, String> params) {
        sessionService.createSession(botRole, p -> {
            botService.addCreatorInfo(botRole, p.messages());
        });

        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BOT_CREATOR_INFO_REQUEST, botRole));
    }
}
