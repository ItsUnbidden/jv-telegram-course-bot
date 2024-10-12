package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;

@Component
@RequiredArgsConstructor
public class Initializer implements ApplicationRunner {
    private final List<MenuConfigurer> menuConfigurers;

    private final TelegramBotsApi api;

    private final TelegramBot bot;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeMenus();

        api.registerBot(bot);
        bot.setUpMenuButton();
        bot.setUpMenus();
    }

    private void initializeMenus() {
        menuConfigurers.forEach(c -> c.configure());
    }
}
