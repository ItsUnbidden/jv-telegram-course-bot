package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Initializer implements ApplicationRunner {
    private static final Logger LOGGER = LogManager.getLogger(Initializer.class);

    private final List<MenuConfigurer> menuConfigurers;

    private final CommandHandlerManager commandHandlerManager;

    private final LocalizationLoader localizationLoader;

    private final UserService userService;

    private final CustomTelegramClient telegramClient;

    private final TelegramBot bot;

    @Override
    public void run(ApplicationArguments args) {
        bot.runSetWebhook();
        
        LOGGER.info("Initializing command menus...");
        telegramClient.setUpMenuButton();
        localizationLoader.getAvailableLanguageCodes().forEach(c ->
                telegramClient.setUpUserMenu(c, commandHandlerManager.getUserCommands()));
        userService.getAdminList().forEach(a -> telegramClient.setUpMenuForAdmin(a,
                commandHandlerManager.getAllCommands()));
        LOGGER.info("Command menus have been initialized.");

        LOGGER.info("Initializing menus...");
        initializeMenus();
        LOGGER.info("Menus have been initialized.");
    }

    private void initializeMenus() {
        menuConfigurers.forEach(c -> c.configure());
    }
}
