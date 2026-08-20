package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.ImageDao;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.Document;
import com.unbidden.telegramcoursesbot.model.content.DocumentContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class ImageFileUploadButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getFormatterLogger(ImageFileUploadButtonHandler.class);

    private final ImageDao imageDao;

    private final ContentSessionService sessionService;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final ValidatorUtil validatorUtil;

    // TODO: this has to be revamped to allow creators to upload course invoice images themselves
    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        sessionService.createSession(user, bot, p -> {
            validatorUtil.checkExactExpectedMessages(user, p.messages(), 2);
            final Long courseId = validatorUtil.parseId(user, p.messages().getLast());

            LOGGER.info("User " + user.getId() + " is trying to update a course " + courseId + " invoice image.");

            final DocumentContent content = (DocumentContent)contentService
                    .parseAndPersistContent(p.user(), p.bot(), p.messages(), List.of(MediaType.DOCUMENT));
            LOGGER.debug("Content " + content.getId() + " will be used for the invoice image "
                    + "for course " + courseId + ".");
            
            final Document document = content.getDocuments().get(0);
            try {
                final File file = clientManager.getBotLordClient().execute(new GetFile(document.getId()));
                final Path path = imageDao.addOrUpdateImage(clientManager.getBotLordClient().downloadFileAsStream(file), courseId);

                LOGGER.info("Invoice image file " + path.toString() + " has been updated.");
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to download file " + document.getId(),
                        localizationLoader.localize(Localizations.Error.DOWNLOAD_FILE, user), e);
            }
            
            LOGGER.info("Invoice image file for course " + courseId + " has been updated.");

            LOGGER.debug("Sending confirmation message...");
            clientManager.getBotLordClient().sendMessage(user, localizationLoader
                    .localize(Localizations.Service.INVOICE_IMAGE_UPDATED, user));
            LOGGER.debug("Message sent.");
        });

        LOGGER.debug("Sending request message...");
        clientManager.getBotLordClient().sendMessage(user, localizationLoader
                .localize(Localizations.Service.INVOICE_IMAGE_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}
