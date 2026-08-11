package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ClientManager {
    private static final Logger LOGGER = LogManager.getLogger(ClientManager.class);

    private static final Map<Long, RegularClient> clients = new HashMap<>(); 

    private volatile boolean isOnMaintenance;

    private volatile boolean isRefreshing;

    private final CertificateDao dao;

    private final UserService userService;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    private final CommandHandlerManager commandHandlerManager;

    private final LocalizationLoader localizationLoader;
    
    private BotLordClient botLordClient;

    public ClientManager(CertificateDao dao, UserService userService,
            LocalizationLoader loader, EntityUtil entityUtil,
            @Lazy CommandHandlerManager commandHandlerManager,
            LocalizationLoader localizationLoader) {
        this.dao = dao;
        this.userService = userService;
        this.loader = loader;
        this.entityUtil = entityUtil;
        this.commandHandlerManager = commandHandlerManager;
        this.localizationLoader = localizationLoader;
    }

    @Value("${telegram.bot.authorization.bot_lord.token}")
    private String botLordToken;

    @Value("${telegram.bot.webhook.url}")
    private String baseUrl;
    
    @Value("${telegram.bot.webhook.secret}")
    private String secretToken;
    
    @Value("${telegram.bot.webhook.ip}")
    private String ip;
    
    @Value("${telegram.bot.webhook.max_connections}")
    private int maxConnections;

    @Value("${telegram.bot.webhook.use_certificate}")
    private boolean isCustomCertificateIncluded;

    public CustomTelegramClient getClient(Bot bot) {
        final CustomTelegramClient client = clients.get(bot.getId());

        if (client == null) {
            if (bot.getId().equals(entityUtil.getBotLord().getId())) {
                return getBotLordClient();
            }
            throw new EntityNotFoundException("Bot " + bot.getId()
                    + "'s client does not exist");
        }
        return client;
    }

    public CustomTelegramClient addClient(Bot bot) {
        LOGGER.debug("Creating a new client for bot " + bot.getId() + "...");
        final RegularClient client = new RegularClient(bot, userService, loader,
                dao, commandHandlerManager, entityUtil, baseUrl, secretToken, ip,
                maxConnections, isCustomCertificateIncluded);

        clients.put(bot.getId(), client);
        return client;
    }

    public BotLordClient addBotLordClient(Bot bot) {
        LOGGER.debug("Creating a new client for bot lord...");
        botLordClient = new BotLordClient(botLordToken, baseUrl, ip, secretToken, bot,
                dao, userService, loader, isCustomCertificateIncluded);
        return botLordClient;
    }

    public void removeClient(Bot bot) {

        LOGGER.info("Remving client for bot " + bot.getId() + "...");
        clients.get(bot.getId()).runDeleteWebhook();
        clients.remove(bot.getId());
        LOGGER.info("Client for bot " + bot.getId() + " has been removed "
                + "and webhook has been deleted.");
    }

    public boolean toggleMaintenance(UserEntity user) {
        if (isRefreshing()) {
            throw new ForbiddenOperationException("Cannot toggle maintenance while the server "
                    + "is refreshing", localizationLoader.localize(
                    Localizations.Error.IS_REFRESHING, user));
        }
        LOGGER.info("Director is toggling maintenance... Current status is "
                + getStatus(user) + ".");
        isOnMaintenance = !isOnMaintenance;
        LOGGER.info("Maintenance is now " + getStatus(user));

        LOGGER.debug("Sending confirmation message to director...");
        getBotLordClient().sendMessage(user, localizationLoader
                .localize(Localizations.Service.ON_MAINTENANCE_STATUS_CHANGE, user,
                    new Localizations.Service.OnMaintenanceStatusChangeParams(getStatus(user))));
        LOGGER.debug("Message sent.");

        return isOnMaintenance;
    }

    public boolean isOnMaintenance() {
        return isOnMaintenance;
    }

    public BotLordClient getBotLordClient() {
        return botLordClient;
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public void setRefreshing(boolean isRefreshing) {
        this.isRefreshing = isRefreshing;
    }

    private String getStatus(UserEntity user) {
        return localizationLoader.localize(isOnMaintenance
                ? Localizations.Service.STATUS_ENABLED
                : Localizations.Service.STATUS_DISABLED, user).getData();
    }
}
