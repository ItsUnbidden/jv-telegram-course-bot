package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class ClientManager {
    private static final Logger LOGGER = LogManager.getLogger(ClientManager.class);

    private static final Map<Long, RegularClient> clients = new HashMap<>(); 

    private volatile boolean isOnMaintenance;

    private volatile boolean isRefreshing;

    private final CertificateDao dao;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    private final CommandHandlerManager commandHandlerManager;

    private final LocalizationLoader localizationLoader;
    
    private BotLordClient botLordClient;

    public ClientManager(CertificateDao dao, LocalizationLoader loader, EntityUtil entityUtil,
            @Lazy CommandHandlerManager commandHandlerManager,
            LocalizationLoader localizationLoader) {
        this.dao = dao;
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
            if (entityUtil.isBotLord(bot.getId())) {
                return getBotLordClient();
            }
            throw new EntityNotFoundException("Bot " + bot.getId()
                    + "'s client does not exist. This is a bug.", null);
        }
        return client;
    }

    public CustomTelegramClient addClient(Bot bot) {
        LOGGER.debug("Creating a new client for bot " + bot.getId() + "...");
        final RegularClient client = new RegularClient(bot.getId(), bot.getToken(), loader, dao, commandHandlerManager,
                entityUtil, baseUrl, secretToken, ip, maxConnections, isCustomCertificateIncluded);

        clients.put(bot.getId(), client);
        return client;
    }

    public BotLordClient addBotLordClient(Bot bot) {
        LOGGER.debug("Creating a new client for bot lord...");
        botLordClient = new BotLordClient(bot.getId(), bot.getToken(), baseUrl, ip, secretToken,
                dao, loader, isCustomCertificateIncluded);
        return botLordClient;
    }

    public void removeClient(Bot bot) {

        LOGGER.info("Remving client for bot " + bot.getId() + "...");
        clients.get(bot.getId()).runDeleteWebhook();
        clients.remove(bot.getId());
        LOGGER.info("Client for bot " + bot.getId() + " has been removed "
                + "and webhook has been deleted.");
    }

    public boolean toggleMaintenance(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null.");

        if (isRefreshing()) {
            throw new ForbiddenOperationException("Cannot toggle maintenance while the server "
                    + "is refreshing", localizationLoader.localize(
                    Localizations.Error.IS_REFRESHING, botRole));
        }
        LOGGER.info("Director is toggling maintenance... Current status is "
                + getStatus(botRole) + ".");
        isOnMaintenance = !isOnMaintenance;
        LOGGER.info("Maintenance is now " + getStatus(botRole));

        LOGGER.debug("Sending confirmation message to director...");
        sendMessage(botRole, localizationLoader.localize(Localizations.Service.ON_MAINTENANCE_STATUS_CHANGE, botRole,
                    new Localizations.Service.OnMaintenanceStatusChangeParams(getStatus(botRole))));
        LOGGER.debug("Message sent.");

        return isOnMaintenance;
    }

    public void refreshMenus(BotRole botRole) {
        if (!isOnMaintenance) {
            throw new ForbiddenOperationException("Unable to refresh because server is not on "
                    + "maintenance", localizationLoader.localize(
                    Localizations.Error.MAINTENANCE_IN_NOT_ENABLED, botRole));
        }
        LOGGER.info("The director is trying to refresh user menus...");

        botLordClient.setUpMenu();
        clients.forEach((k, v) -> v.reloadMenus());
        LOGGER.info("Menus have been reloaded.");

        LOGGER.debug("Sending confirmation message...");
        sendMessage(botRole, localizationLoader.localize(Localizations.Service.MENU_REFRESH_SUCCESS, botRole));
        LOGGER.debug("Message sent.");
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

    /**
     * Sends a message using {@link SendMessage} to the chat specified in the {@link BotRole}. 
     * There are three possible result types:
     * <ls>
     *  <li>OK -> Sent successfully</li>
     *  <li>SKIPPED -> User has disabled the bot</li>
     *  <li>FAILURE -> An unexpected error occured</li>
     * </ls>
     * 
     * @param botRole
     * @param sendMessage
     * @return the result wrapped in {@link SendMessageResultDto}
     */
    public SendMessageResultDto sendMessage(BotRole botRole, SendMessage sendMessage) {
        if (botRole.isDisabled()) {
            return new SendMessageResultDto();
        }

        try {
            return new SendMessageResultDto(getClient(botRole.getBot()).execute(sendMessage));
        } catch (TelegramApiException e) {
            return new SendMessageResultDto(new TelegramException("Unable to send a message to user " + botRole.getUser().getId()
                    + " in bot " + botRole.getBot().getId() + ".", localizationLoader.localize(Localizations.Error.SEND_MESSAGE,
                        botRole), e));
        }
    }

    /**
     * Asynchronously sends message provided in {@link SendMessage}. Warning! Field chatId in 
     * {@link SendMessage} must be a user id, if that is not the case, exception will be thrown.
     * @param sendMessage Telegram message builder
     * @return {@link CompletableFuture} of the {@link Message}
     */
    public CompletableFuture<SendMessageResultDto> sendMessageAsync(BotRole botRole, SendMessage sendMessage) {
        if (botRole.isDisabled()) {
            return CompletableFuture.completedFuture(new SendMessageResultDto());
        }

        try {
            return getClient(botRole.getBot()).executeAsync(sendMessage).handle((m, t) -> {
                        if (t != null) {
                            return new SendMessageResultDto(new TelegramException("Unable to send a message to user " + botRole.getUser().getId()
                                + " in bot " + botRole.getBot().getId() + ".", localizationLoader.localize(Localizations.Error.SEND_MESSAGE,
                                botRole), t));
                        } else {
                            return new SendMessageResultDto(m);
                        }
                    });
        } catch (TelegramApiException e) {
            return CompletableFuture.completedFuture(new SendMessageResultDto(new TelegramException("Unable to send a message to user "
                    + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".", localizationLoader.localize(
                    Localizations.Error.SEND_MESSAGE, botRole), e)));
        }
    }

    /**
     * Sends a message with the provided {@link Localization} to the chat specified in the {@link BotRole}. 
     * There are three possible result types:
     * <ls>
     *  <li>OK -> Sent successfully</li>
     *  <li>SKIPPED -> User has disabled the bot</li>
     *  <li>FAILURE -> An unexpected error occured</li>
     * </ls>
     * 
     * @param botRole
     * @param localization
     * @return the result wrapped in {@link SendMessageResultDto}
     */
    public SendMessageResultDto sendMessage(BotRole botRole, Localization localization) {
        if (botRole.isDisabled()) {
            return new SendMessageResultDto();
        }

        try {
            return new SendMessageResultDto(getClient(botRole.getBot()).execute(SendMessage.builder()
                    .chatId(botRole.getUser().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build()));
        } catch (TelegramApiException e) {
            return new SendMessageResultDto(new TelegramException("Unable to send a message to user " + botRole.getUser().getId()
                    + " in bot " + botRole.getBot().getId() + ".", localizationLoader.localize(Localizations.Error.SEND_MESSAGE,
                        botRole), e));
        }
    }

    /**
     * Sends a message with the provided {@link Localization} and markup to the chat specified in the {@link BotRole}.
     * There are three possible result types:
     * <ls>
     *  <li>OK -> Sent successfully</li>
     *  <li>SKIPPED -> User has disabled the bot</li>
     *  <li>FAILURE -> An unexpected error occured</li>
     * </ls>
     * 
     * @param botRole
     * @param localization
     * @param markup
     * @return the result wrapped in {@link SendMessageResultDto}
     */
    public SendMessageResultDto sendMessage(BotRole botRole, Localization localization, ReplyKeyboard markup) {
        if (botRole.isDisabled()) {
            return new SendMessageResultDto();
        }

        try {
            return new SendMessageResultDto(getClient(botRole.getBot()).execute(SendMessage.builder()
                    .chatId(botRole.getUser().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .replyMarkup(markup)
                    .build()));
        } catch (TelegramApiException e) {
            return new SendMessageResultDto(new TelegramException("Unable to send a message to user " + botRole.getUser().getId()
                    + " in bot " + botRole.getBot().getId() + ".", localizationLoader.localize(Localizations.Error.SEND_MESSAGE,
                        botRole), e));
        }
    }

    /**
     * Asynchronously sends a message with the provided {@link Localization} to the chat specified in the {@link BotRole}. 
     * The future <b>always completes</b>. There are three possible result types:
     * <ls>
     *  <li>OK -> Sent successfully</li>
     *  <li>SKIPPED -> User has disabled the bot</li>
     *  <li>FAILURE -> An unexpected error occured</li>
     * </ls>
     * 
     * @param botRole
     * @param localization
     * @return {@link CompletableFuture} with the result wrapped in {@link SendMessageResultDto}
     */
    public CompletableFuture<SendMessageResultDto> sendMessageAsync(BotRole botRole, Localization localization) {
        if (botRole.isDisabled()) {
            return CompletableFuture.completedFuture(new SendMessageResultDto());
        }

        try {
            return getClient(botRole.getBot()).executeAsync(SendMessage.builder()
                    .chatId(botRole.getUser().getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build()).handle((m, t) -> {
                        if (t != null) {
                            return new SendMessageResultDto(new TelegramException("Unable to send a message to user " + botRole.getUser().getId()
                                + " in bot " + botRole.getBot().getId() + ".", localizationLoader.localize(Localizations.Error.SEND_MESSAGE,
                                botRole), t));
                        } else {
                            return new SendMessageResultDto(m);
                        }
                    });
        } catch (TelegramApiException e) {
            return CompletableFuture.completedFuture(new SendMessageResultDto(new TelegramException("Unable to send a message to user "
                    + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".", localizationLoader.localize(
                    Localizations.Error.SEND_MESSAGE, botRole), e)));
        }
    }

    private String getStatus(BotRole botRole) {
        return localizationLoader.localize(isOnMaintenance
                ? Localizations.Service.STATUS_ENABLED
                : Localizations.Service.STATUS_DISABLED, botRole).getData();
    }
}
