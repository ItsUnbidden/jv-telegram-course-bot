package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.config.properties.WebhookProperties;
import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;

import java.io.InputStream;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.GetWebhookInfo;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.WebhookInfo;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands;
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

    protected final Long botId;

    protected final LocalizationLoader localizationLoader;

    protected final WebhookProperties webhookProperties;

    private final CertificateDao certificateDao;

    /**
     * Creates a new instance of {@link CustomTelegramClient}.
     * @param botId of the bot this client represents
     * @param botToken of the bot this client represents
     * @param loader
     * @param dao to load a custom certificate
     * @param baseUrl of this server
     * @param secretToken to check whether an update comes from an actual telegram server
     * @param ip if no DNS is used
     * @param isCustomCertificateIncluded whether custom sertificate is included
     * (dao is still required)
     * @author Unbidden
     */
    public CustomTelegramClient(Long botId, String botToken, LocalizationLoader loader, CertificateDao dao, WebhookProperties webhookProperties) {
        super(botToken);

        this.botId = botId;
        this.localizationLoader = loader;
        this.logger = LogManager.getLogger("Bot " + botId + "'s Client");
        this.certificateDao = dao;
        this.webhookProperties = webhookProperties;
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
    public void runSetWebhook(String endpoint) {
        logger.info("Registering webhook bot...");
        InputStream publicKeyStream = null;
        if (webhookProperties.useCertificate()) {
            logger.info("Using a custom certificate...");
            publicKeyStream = certificateDao.readPublicKey();
            logger.debug("Certificate public key file has been initialized into stream.");
        }
        try {
            execute(SetWebhook.builder()
                    .url(webhookProperties.url() + endpoint)
                    .certificate((webhookProperties.useCertificate()) ? new InputFile(publicKeyStream,
                        CertificateDao.PUBLIC_KEY_FILE_NAME) : null)
                    .ipAddress((webhookProperties.ip() == null || webhookProperties.ip().equals("") ? null : webhookProperties.ip()))
                    .secretToken(webhookProperties.secret())
                    .maxConnections(webhookProperties.maxConnections())
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up bot " + botId
                    + "'s webhook.", null, e);
        } finally {
            if (publicKeyStream != null) {
                certificateDao.closeStream(publicKeyStream);
                logger.debug("Certificate public key stream has been closed.");
            }
        }
        logger.info("Bot " + botId + " has been registered.");
    }

    protected List<BotCommand> parseToBotCommands(List<String> commands, String languageCode, List<String> customLangPriority) {
        return commands.stream()
                .map(c -> (BotCommand)BotCommand.builder()
                    .command(c)
                    .description(localizationLoader.loadGenericLocalization(Localizations.Menu.COMMAND_DESCRIPTION,
                        languageCode, customLangPriority, c.replace("/", "").toLowerCase()).getData())
                    .build())
                .toList();
    }

    protected void initialize(String endpoint) {
        runDeleteWebhook();
        runSetWebhook(endpoint);

        setUpMenuButton();
    }
}
