package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.ImageDao;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteInvoiceImageButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            DeleteInvoiceImageButtonHandler.class);

    private static final String SERVICE_INVOICE_IMAGE_DELETE_REQUEST =
            "service_invoice_image_delete_request";
    private static final String SERVICE_INVOICE_IMAGE_DELETED = "service_invoice_image_deleted";

    private static final String ERROR_INVOICE_IMAGE_DOES_NOT_EXIST =
            "error_invoice_image_does_not_exist";

    private static final int EXPECTED_MESSAGES = 1;

    private final ImageDao imageDao;
    
    private final ContentSessionService sessionService;

    private final BotService botService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);

        sessionService.createSession(user, bot, m -> {
            textUtil.checkExpectedMessages(EXPECTED_MESSAGES, user, m, localizationLoader);

            final String courseName = m.get(0).getText();
            if (!imageDao.exists(courseName)) {
                throw new InvalidDataSentException("Invoice image does not exist for course "
                        + courseName, localizationLoader.getLocalizationForUser(
                        ERROR_INVOICE_IMAGE_DOES_NOT_EXIST, user));
            }

            LOGGER.info("Deleing invoice image for course " + courseName + "...");
            imageDao.delete(courseName);
            LOGGER.info("Image deleted.");
            LOGGER.debug("Sending confirmation message...");
            clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_INVOICE_IMAGE_DELETED, user));
            LOGGER.debug("Message sent.");
        }, true);
        LOGGER.debug("Sending request message...");
        clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_INVOICE_IMAGE_DELETE_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}
