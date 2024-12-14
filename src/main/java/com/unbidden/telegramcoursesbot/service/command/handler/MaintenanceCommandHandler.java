package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class MaintenanceCommandHandler implements CommandHandler {
    private static final String ERROR_IS_REFRESHING = "error_is_refreshing";

    private static final Logger LOGGER = LogManager.getLogger(MaintenanceCommandHandler.class);

    private static final String COMMAND = "/maintenance";
    
    private static final String PARAM_STATUS = "${status}";
    
    private static final String SERVICE_ON_MAINTENANCE_STATUS_CHANGE =
            "service_on_maintenance_status_change";
    private static final String SERVICE_STATUS_DISABLED = "service_status_disabled";
    private static final String SERVICE_STATUS_ENABLED = "service_status_enabled";
    
    private final ClientManager clientManager;

    private final BotService botService;

    private final LocalizationLoader localizationLoader;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Message message,
            @NonNull String[] commandParts) {
        botService.checkBotFather(bot, user);
        if (clientManager.isRefreshing()) {
            throw new ForbiddenOperationException("Cannot toggle maintenance while the server "
                    + "is refreshing", localizationLoader.getLocalizationForUser(
                    ERROR_IS_REFRESHING, user));
        }
        LOGGER.info("Director is toggling maintenance... Current status is "
                + getStatus(clientManager.isOnMaintenance()) + ".");
        clientManager.toggleMaintenance();
        LOGGER.info("Maintenance is now " + getStatus(clientManager.isOnMaintenance()));
        LOGGER.debug("Sending confirmation message to director...");
        clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_ON_MAINTENANCE_STATUS_CHANGE, user,
                PARAM_STATUS, getStatus(user, clientManager.isOnMaintenance())));
        LOGGER.debug("Message sent.");
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    @NonNull
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.MAINTENANCE);
    }

    private String getStatus(UserEntity user, boolean isOnMaintenance) {
        return (isOnMaintenance) ? localizationLoader
                .getLocalizationForUser(SERVICE_STATUS_ENABLED, user).getData()
                : localizationLoader.getLocalizationForUser(SERVICE_STATUS_DISABLED, user)
                .getData();
    }

    private String getStatus(boolean isOnMaintenance) {
        return (isOnMaintenance) ? "ENABLED" : "DISABLED";
    }
}
