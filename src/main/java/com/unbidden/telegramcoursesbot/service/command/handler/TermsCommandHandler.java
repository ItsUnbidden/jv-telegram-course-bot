package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class TermsCommandHandler implements CommandHandler {
    private static final String COMMAND = "/terms";

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.INFO)
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        if (bot.getTerms() == null) {
            clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                    Localizations.Service.NO_TERMS, user));
            return;
        }
        contentService.sendLocalizedContent(user, bot, bot.getTerms().getId());
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
