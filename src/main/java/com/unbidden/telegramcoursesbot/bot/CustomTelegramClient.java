package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.GetWebhookInfo;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.WebhookInfo;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public abstract class CustomTelegramClient extends OkHttpTelegramClient {
    protected static final List<String> BOT_LORD_COMMANDS = List.of(
        "/maintenance",
        "/refresh",
        "/generalban",
        "/botsettings",
        "/generalpost",
        "/terminatemenu",
        "/files"
    );

    protected final Logger logger;

    protected final Bot bot;

    protected final LocalizationLoader localizationLoader;

    private final CertificateDao certificateDao;

    private final String baseUrl;

    private final String secretToken;

    private final String ip;

    private final boolean isCustomCertificateIncluded;

    /**
     * Creates a new instance of {@link CustomTelegramClient}.
     * @param bot this client is supposed to service
     * @param userService
     * @param loader
     * @param dao to load a custom certificate
     * @param baseUrl of this server
     * @param secretToken to check whether an update comes from an actual telegram server
     * @param ip if no DNS is used
     * @param isCustomCertificateIncluded whether custom sertificate is included
     * (dao is still required)
     * @author Unbidden
     */
    public CustomTelegramClient(Bot bot, LocalizationLoader loader, CertificateDao dao, String baseUrl,
            String secretToken, @Nullable String ip, boolean isCustomCertificateIncluded) {
        super(bot.getToken());

        this.bot = bot;
        this.localizationLoader = loader;
        this.logger = LogManager.getLogger("Bot " + bot.getId() + "'s Client");
        this.certificateDao = dao;
        this.baseUrl = baseUrl;
        this.secretToken = secretToken;
        this.ip = ip;
        this.isCustomCertificateIncluded = isCustomCertificateIncluded;
    }

    public WebhookInfo getInfo() {
        try {
            return execute(GetWebhookInfo.builder().build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to get webhook info.", null, e);
        }
    }

    public void setUpMenuButton() {
        final SetChatMenuButton setChatMenuButton = SetChatMenuButton.builder()
                .menuButton(MenuButtonCommands.builder().build())
                .build();
        try {
            execute(setChatMenuButton);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set bot's menu button.", null, e);
        }
    }

    /**
     * Sends message provided in {@link SendMessage}. Warning! Field chatId in 
     * {@link SendMessage} must be a user id, if that is not the case, exception will be thrown.
     * @param sendMessage Telegram message builder
     * @return sent {@link Message}
     */
    public Message sendMessage(SendMessage sendMessage) {
        try {
            return execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Unable to send message.", e);
            return new Message(); // TODO: ensure that this is a reasonable approach
        }
    }

    /**
     * Asynchronously sends message provided in {@link SendMessage}. Warning! Field chatId in 
     * {@link SendMessage} must be a user id, if that is not the case, exception will be thrown.
     * @param sendMessage Telegram message builder
     * @return {@link CompletableFuture} of the {@link Message}
     */
    public CompletableFuture<Message> sendMessageAsync(SendMessage sendMessage) {
        try {
            return executeAsync(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Unable to send message.", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sends message to {@link UserEntity} using provided {@link Localization}.
     * @param user to whom the message will be sent
     * @param localization
     * @return sent {@link Message}
     */
    public Message sendMessage(UserEntity user, Localization localization) {
        try {
            return execute(SendMessage.builder()
                    .chatId(user.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        } catch (TelegramApiException e) {
            logger.error("Unable to send message to user %s.".formatted(user.getId()), e);
            return new Message(); // TODO: ensure that this is a reasonable approach
        }
    }

    /**
     * Asynchronously sends message to {@link UserEntity} using provided {@link Localization}.
     * @param user to whom the message will be sent
     * @param localization
     * @return {@link CompletableFuture} of the {@link Message}
     */
    public CompletableFuture<Message> sendMessageAsync(UserEntity user, Localization localization) {
        try {
            return executeAsync(SendMessage.builder()
                    .chatId(user.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        } catch (TelegramApiException e) {
            logger.error("Unable to send message to user %s.".formatted(user.getId()), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sends message to {@link UserEntity} using provided {@link Localization}
     * with a specified markup.
     * @param user to whom the message will be sent
     * @param localization
     * @param replyMarkup 
     * @return sent {@link Message}
     */
    public Message sendMessage(UserEntity user, Localization localization,
            ReplyKeyboard replyMarkup) {
        try {
            return execute(SendMessage.builder()
                    .chatId(user.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .replyMarkup(replyMarkup)
                    .build());
        } catch (TelegramApiException e) {
            logger.error("Unable to send message to user %s.".formatted(user.getId()), e);
            return new Message(); // TODO: ensure that this is a reasonable approach
        }
    }

    public void runDeleteWebhook() {
        try {
            execute(DeleteWebhook.builder()
                    .dropPendingUpdates(true)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to delete webhook", null, e);
        }
    }

    /**
     * Sets a webhook for this client's bot
     * @param endpoint must begin with a '/'
     * @param maxConnections
     */
    public void runSetWebhook(String endpoint, Integer maxConnections) {
        Assert.notNull(baseUrl, "Base url cannot be null");
        Assert.notNull(secretToken, "Due to security reasons secret token cannot be null");

        logger.info("Registering webhook bot...");
        InputStream publicKeyStream = null;
        if (isCustomCertificateIncluded) {
            logger.info("Using a custom certificate...");
            publicKeyStream = certificateDao.readPublicKey();
            logger.debug("Certificate public key file has been initialized into stream.");
        }
        try {
            execute(SetWebhook.builder()
                    .url(baseUrl + endpoint)
                    .certificate((isCustomCertificateIncluded) ? new InputFile(publicKeyStream,
                        CertificateDao.PUBLIC_KEY_FILE_NAME) : null)
                    .ipAddress((ip == null || ip.equals("") ? null : ip))
                    .secretToken(secretToken)
                    .maxConnections(maxConnections)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up bot " + bot.getId()
                    + "'s webhook.", null, e);
        } finally {
            if (publicKeyStream != null) {
                certificateDao.closeStream(publicKeyStream);
                logger.debug("Certificate public key stream has been closed.");
            }
        }
        logger.info("Bot " + bot.getId() + " has been registered.");
    }

    protected List<BotCommand> parseToBotCommands(List<String> commands, String languageCode) {
        return commands.stream()
                .map(c -> (BotCommand)BotCommand.builder()
                    .command(c)
                    .description(localizationLoader.loadGenericLocalization(Localizations.Menu.COMMAND_DESCRIPTION,
                        languageCode, c.replace("/", "").toLowerCase()).getData())
                    .build())
                .toList();
    }

    protected void initialize(String endpoint, Integer maxConnections) {
        runDeleteWebhook();
        runSetWebhook(endpoint, maxConnections);

        setUpMenuButton();
    }
}
