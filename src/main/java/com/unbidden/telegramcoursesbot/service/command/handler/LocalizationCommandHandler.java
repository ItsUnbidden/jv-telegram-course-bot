package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.LocalizationKey;
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
        if (commandParts.length > 3) {
            try {
                final Localization localization = localizationLoader.loadLocalization(getKey(user, commandParts[1], commandParts[2]), commandParts[3]);
    
                clientManager.getClient(bot).sendMessage(user, localization);
            } catch (RuntimeException e) {
                throw new InvalidDataSentException("Localization " + commandParts[1] + " for language code " + commandParts[2]
                        + " does not exist.", localizationLoader.localize(Localizations.Error.LOCALIZATION_DOES_NOT_EXIST, user));
            }
        } else {
            throw new InvalidDataSentException("Localization command requires at least three "
                    + "params: 1. Localization type, 2. Localization name, 3. Language code (en, ru, etc.)",
                    localizationLoader.localize(Localizations.Error.LOCALIZATION_PARAMS_INVALID,
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

    private LocalizationKey getKey(UserEntity user, String type, String name) {
        final String typeUpper = type.toUpperCase();
        final String nameUpper = name.toUpperCase();

        try {
            switch (typeUpper) {
                case "ERROR" -> {
                    return Localizations.Error.valueOf(nameUpper);
                }
                case "SERVICE" -> {
                    return Localizations.Service.valueOf(nameUpper);
                }
                case "BUTTON" -> {
                    return Localizations.Button.valueOf(nameUpper);
                }
                case "MENU" -> {
                    return Localizations.Button.valueOf(nameUpper);
                }
                default -> {
                    throw new InvalidDataSentException("Localization type " + typeUpper + " does not exist.",
                            localizationLoader.localize(Localizations.Error.LOCALIZATIONS_KEY_PARSE_FAILURE, user));
                }
            }
        } catch (Exception e) {
            throw new InvalidDataSentException("Unable to parse localization name to a key.",
                    localizationLoader.localize(Localizations.Error.LOCALIZATIONS_KEY_PARSE_FAILURE, user));
        }
    }
}
