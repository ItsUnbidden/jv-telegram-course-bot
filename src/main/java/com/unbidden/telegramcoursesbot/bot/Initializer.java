package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
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

    private final BotService botService;

    private final UserService userService;

    private final CourseService courseService;

    @Override
    public void run(ApplicationArguments args) {
        // Initializing director
        final UserEntity director = userService.createDummyDirector();

        // Initializing botfather and its client
        botService.initializeBotFather(botService.createBotFather(director));
    
        // Initializing initial bot and course enities
        courseService.createInitialCourse(botService.createInitialBot(director));

        // Initializing clients
        botService.initializeBots();

        // Initilizing interface menu schemes
        LOGGER.info("Initializing menus...");
        menuConfigurers.forEach(c -> c.configure());
        LOGGER.info("Menus have been initialized.");
    }
}
