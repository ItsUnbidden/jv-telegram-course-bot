package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class LocalizationCommandHandler implements CommandHandler {
    private static final String COMMAND = "/localization";

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = {AuthorityType.MAINTENANCE})
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        if (commandParts.length > 2) {
            try {
                final Localization localization = localizationLoader.loadLocalization(Localizations.getKeyByLocName(commandParts[1]),
                        commandParts[2]);
    
                clientManager.getClient(bot).sendMessage(user, localization);
            } catch (RuntimeException e) {
                throw new InvalidDataSentException("Localization " + commandParts[1] + " for language code " + commandParts[2]
                        + " does not exist.", localizationLoader.getLocalizationForUser(Localizations.Error.LOCALIZATION_DOES_NOT_EXIST, user));
            }
        } else {
            throw new InvalidDataSentException("Localization command requires at least two "
                    + "params: 1. Localization name, 2. Language code (en, ru, etc.)",
                    localizationLoader.getLocalizationForUser(Localizations.Error.LOCALIZATION_PARAMS_INVALID,
                    user));
        }
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.CONTENT_SETTINGS);
    }
}
