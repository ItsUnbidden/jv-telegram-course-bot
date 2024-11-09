package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.dao.CertificateDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.webhook.TelegramWebhookBot;

@Component
public class TelegramBot implements TelegramWebhookBot {
    private static final Logger LOGGER = LogManager.getLogger(TelegramBot.class);

    private static final String UPDATE_ENDPOINT_PATH = "bot";

    @Autowired
    private TelegramClient telegramClient;

    @Autowired
    private CertificateDao certificateDao;

    @Value("${telegram.bot.webhook.url}")
    private String baseUrl;

    @Value("${telegram.bot.webhook.secret}")
    private String secretToken;

    @Value("${telegram.bot.webhook.ip}")
    private String ip;

    @Value("${telegram.bot.webhook.max-connections}")
    private Integer maxConnections;

    @Override
    public void runDeleteWebhook() {
        try {
            telegramClient.execute(DeleteWebhook.builder()
                    .dropPendingUpdates(true)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to delete webhook", null, e);
        }
    }

    @Override
    public void runSetWebhook() {
        Assert.notNull(baseUrl, "Base url cannot be null");
        Assert.notNull(secretToken, "Due to security reasons secret token cannot be null");

        LOGGER.info("Registering webhook bot...");
        final InputStream publicKeyStream = certificateDao.readPublicKey();
        LOGGER.debug("Certificate public key file has been initialized into stream.");
        try {
            telegramClient.execute(SetWebhook.builder()
                    .url(baseUrl + "/webhook/callback/" + getBotPath())
                    .certificate(new InputFile(publicKeyStream,
                        CertificateDao.PUBLIC_KEY_FILE_NAME))
                    .ipAddress(ip)
                    .secretToken(secretToken)
                    .maxConnections(maxConnections)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up the webhook bot.", null, e);
        } finally {
            certificateDao.closeStream(publicKeyStream);
            LOGGER.debug("Certificate public key stream has been closed.");
        }
        LOGGER.info("Bot has been registered.");
    }

    @Override
    public BotApiMethod<?> consumeUpdate(Update update) {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public String getBotPath() {
        return UPDATE_ENDPOINT_PATH;
    }
}
