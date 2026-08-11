package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
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

    private final BotService botService;

    private final UserService userService;

    private final MenuService menuService;

    @Override
    public void run(ApplicationArguments args) {
        // Initializing director
        final UserEntity director = userService.createDummyDirector();

        // Initializing bot lord and its client
        botService.initializeBotLord(botService.updateBotLord(director));
    
        // Initializing initial bot and course enities
        botService.updateInitialBot(director);

        // Initializing clients
        botService.initializeBots();

        // Initilizing interface menu schemes
        LOGGER.info("Initializing menus...");
        menuConfigurers.forEach(c -> menuService.save(c.configure()));
        LOGGER.info("Menus have been initialized.");
    }
}
