package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class Initializer implements ApplicationRunner {
    private static final Logger LOGGER = LogManager.getLogger(Initializer.class);

    private final CertificateDao certificateDao;

    private final List<MenuConfigurer> menuConfigurers;

    private final CommandHandlerManager commandHandlerManager;

    private final LocalizationLoader localizationLoader;

    private final UserService userService;

    private final TelegramBotsApi api;

    private final TelegramBot bot;

    @Value("${telegram.bot.webhook.url}")
    private String baseUrl;

    @Value("${telegram.bot.webhook.secret}")
    private String secretToken;

    @Value("${telegram.bot.webhook.ip}")
    private String ip;

    @Value("${telegram.bot.webhook.max-connections}")
    private Integer maxConnections;

    @Override
    public void run(ApplicationArguments args) {
        Assert.notNull(baseUrl, "Base url cannot be null.");
        Assert.notNull(secretToken, "Due to security reasons secret token cannot be null.");

        LOGGER.info("Registering webhook bot...");
        final InputStream publicKeyStream = certificateDao.readPublicKey();
        LOGGER.info("Certificate public key file has been initialized into stream.");
        try {
            api.registerBot(bot, SetWebhook.builder().url(baseUrl + "/webhook")
                    .certificate(new InputFile(publicKeyStream, CertificateDao.PUBLIC_KEY_FILE_NAME))
                    .ipAddress(ip)
                    .secretToken(secretToken)
                    .maxConnections(maxConnections)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up the webhook bot.", null, e);
        } finally {
            certificateDao.closeStream(publicKeyStream);
            LOGGER.info("Certificate public key stream has been closed.");
        }
        LOGGER.info("Bot has been registered.");
        
        LOGGER.info("Initializing command menus...");
        bot.setUpMenuButton();
        localizationLoader.getAvailableLanguageCodes().forEach(c -> bot.setUpUserMenu(c,
                commandHandlerManager.getUserCommands()));
        userService.getAdminList().forEach(a -> bot.setUpMenuForAdmin(a,
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
