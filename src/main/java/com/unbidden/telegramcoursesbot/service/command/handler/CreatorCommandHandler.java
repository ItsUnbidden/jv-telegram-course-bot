package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.model.BotRole;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class CreatorCommandHandler implements CommandHandler {
    private static final String COMMAND = "/creator";
    
    private final ContentOrchestrationService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.INFO)
    public void handle(BotRole botRole, Message message, String[] commandParts) {
        if (botRole.getBot().getCreatorInfo() == null) {
            clientManager.sendMessage(botRole, localizationLoader
                    .localize(Localizations.Service.NO_CREATOR_INFO, botRole));
            return;
        }
        contentService.sendLocalizedContent(botRole, botRole.getBot().getCreatorInfo().getId());
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
