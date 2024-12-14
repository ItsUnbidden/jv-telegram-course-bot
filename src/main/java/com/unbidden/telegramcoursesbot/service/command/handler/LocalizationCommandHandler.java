package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class LocalizationCommandHandler implements CommandHandler {
    private static final String ERROR_LOCALIZATION_PARAMS_INVALID = "error_localization_params_invalid";

    private static final String COMMAND = "/localization";

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = {AuthorityType.CONTENT_SETTINGS})
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Message message,
            @NonNull String[] commandParts) {
        if (commandParts.length > 2) {
            final Localization localization = localizationLoader.loadLocalization(commandParts[1],
                    commandParts[2]);

            clientManager.getClient(bot).sendMessage(user, localization);
        } else {
            throw new InvalidDataSentException("Localization command requires at least two "
                    + "params: 1. Localization name, 2. Language code (en, ru, etc.)",
                    localizationLoader.getLocalizationForUser(ERROR_LOCALIZATION_PARAMS_INVALID,
                    user));
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
        return List.of(AuthorityType.CONTENT_SETTINGS);
    }
}
