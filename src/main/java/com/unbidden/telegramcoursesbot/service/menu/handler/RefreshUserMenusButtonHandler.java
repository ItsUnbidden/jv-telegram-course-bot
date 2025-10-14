package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.bot.RegularClient;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshUserMenusButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            RefreshUserMenusButtonHandler.class);

    private static final String SERVICE_MENU_REFRESH_SUCCESS = "service_menu_refresh_success";

    private static final String ERROR_MAINTENANCE_IN_NOT_ENABLED =
            "error_maintenance_in_not_enabled";

    private final BotService botService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);
        if (!clientManager.isOnMaintenance()) {
            throw new ForbiddenOperationException("Unable to refresh because server is not on "
                    + "maintenance", localizationLoader.getLocalizationForUser(
                    ERROR_MAINTENANCE_IN_NOT_ENABLED, user));
        }
        LOGGER.info("The director is trying to refresh user menus...");

        clientManager.getBotFatherClient().setUpMenu();
        botService.getAllBots().forEach(b -> ((RegularClient)clientManager.getClient(b))
                .reloadMenus());
        LOGGER.info("Menus have been reloaded.");
        LOGGER.debug("Sending confirmation message...");
        clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_MENU_REFRESH_SUCCESS, user));
    }
}
