package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientManagerImpl implements ClientManager {
    private static final Logger LOGGER = LogManager.getLogger(ClientManagerImpl.class);

    private static final Map<String, RegularClient> clients = new HashMap<>(); 

    private volatile boolean isOnMaintenance;

    private volatile boolean isRefreshing;

    private final CertificateDao dao;

    private final UserService userService;

    private final LocalizationLoader loader;
    
    private BotFatherClient botFatherClient;

    @Autowired
    @Lazy
    private CommandHandlerManager commandHandlerManager;

    @Value("${telegram.bot.authorization.bot_father.token}")
    private String botFatherToken;

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

    @Override
    @NonNull
    public CustomTelegramClient getClient(@NonNull Bot bot) {
        final CustomTelegramClient client = clients.get(bot.getName());

        if (client == null) {
            if (bot.getName().equals(botFatherClient.getBotName())) {
                return getBotFatherClient();
            }
            throw new EntityNotFoundException("Bot " + bot.getName()
                    + "'s client does not exist");
        }
        return client;
    }

    @Override
    @NonNull
    public CustomTelegramClient addClient(@NonNull Bot bot) {
        LOGGER.debug("Creating a new client for bot " + bot.getName() + "...");
        final RegularClient client = new RegularClient(bot, userService, loader,
                dao, commandHandlerManager, baseUrl, secretToken, ip,
                maxConnections, isCustomCertificateIncluded);

        clients.put(bot.getName(), client);
        return client;
    }

    @Override
    @NonNull
    public BotFatherClient addBotFatherClient(@NonNull Bot bot) {
        LOGGER.debug("Creating a new client for botfather...");
        botFatherClient = new BotFatherClient(botFatherToken, baseUrl, ip, secretToken, bot,
                dao, userService, loader, isCustomCertificateIncluded);
        return botFatherClient;
    }

    @Override
    public void removeClient(@NonNull Bot bot) {

        LOGGER.info("Remving client for bot " + bot.getName() + "...");
        clients.get(bot.getName()).runDeleteWebhook();
        clients.remove(bot.getName());
        LOGGER.info("Client for bot " + bot.getName() + " has been removed "
                + "and webhook has been deleted.");
    }

    @Override
    public boolean toggleMaintenance() {
        isOnMaintenance = !isOnMaintenance;
        LOGGER.info("Maintenance has been toggled to " + isOnMaintenance + ".");
        return isOnMaintenance;
    }

    @Override
    public boolean isOnMaintenance() {
        return isOnMaintenance;
    }

    @Override
    @NonNull
    public BotFatherClient getBotFatherClient() {
        return botFatherClient;
    }

    @Override
    public boolean isRefreshing() {
        return isRefreshing;
    }

    @Override
    public void setRefreshing(boolean isRefreshing) {
        this.isRefreshing = isRefreshing;
    }
}
