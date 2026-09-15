package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.UserEntity;
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

    private final BotOrchestrationService botService;

    private final UserService userService;

    private final MenuOrchestrationService menuService;

    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("Initializing director...");
        final UserEntity director = userService.createDummyDirector();
        
        LOGGER.info("Director has been initialized. Initializing bot lord and its client...");
        botService.initializeBotLord(botService.updateBotLord(director));
    
        LOGGER.info("Bot lord and its client have been initialized. Initializing initial bot...");
        botService.updateInitialBot(director);

        LOGGER.info("Initial bot has been initialized. Initializing regular clients...");
        botService.initializeBots();

        LOGGER.info("Regular clients have been initialized. Initializing menu schemas...");
        menuConfigurers.forEach(c -> menuService.save(c.configure()));
        LOGGER.info("Menus have been initialized.");
    }
}
