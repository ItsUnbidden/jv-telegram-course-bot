package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.LocalizationDao;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
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
import com.unbidden.telegramcoursesbot.service.content.ContentService;
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
public class LocalizationFileUploadButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(LocalizationFileUploadButtonHandler.class);

    private final LocalizationDao localizationDao;

    private final ContentSessionService sessionService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final ValidatorUtil validatorUtil;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(UserEntity director, Bot botlord, Map<String, String> params) {
        sessionService.createSession(director, botlord, p -> {
            LOGGER.info("Director " + p.user().getId() + " is trying to update application "
                    + "localizations files.");

            validatorUtil.checkAtLeastExpectedMessages(director, p.messages(), 2);
            if (!validatorUtil.checkLanguageCode(director, p.messages().getLast())) {
                throw new InvalidDataSentException("Uploading new localization files requires specifying the localization code.",
                        localizationLoader.localize(Localizations.Error.LANGUAGE_CODE_REQUIRED, director));
            }
            final String languageCode = p.messages().getLast().getText().trim();
            final DocumentContent content = (DocumentContent)contentService
                    .parseAndPersistContent(director, botlord, p.messages(), List.of(MediaType.DOCUMENT));

            LOGGER.debug("Content " + content.getId() + " will be used for localization files. "
                    + "There are " + content.getDocuments().size() + " documents present.");
            
            localizationDao.createLanguageSubDir(languageCode);
            for (final Document document : content.getDocuments()) {
                validatorUtil.checkIfDocumentIsALocalization(director, document);

                try {
                    final File file = clientManager.getBotLordClient().execute(new GetFile(document.getId()));
                    final Path path = localizationDao.addOrUpdateLocalizationsFile(
                            clientManager.getBotLordClient().downloadFileAsStream(file),
                            document.getFileName(), languageCode);

                    LOGGER.info("Localization file " + path.toString() + " has been updated.");
                } catch (TelegramApiException e) {
                    throw new TelegramException("Unable to download file " + document.getId(),
                            localizationLoader.localize(Localizations.Error.DOWNLOAD_FILE, director), e);
                }
            }
            LOGGER.info(content.getDocuments().size() + " localization files have been updated.");

            LOGGER.debug("Sending confirmation message...");
            clientManager.getBotLordClient().sendMessage(director, localizationLoader
                    .localize(Localizations.Service.LOCALIZATION_FILES_UPDATED, director,
                        new Localizations.Service.LocalizationFilesUpdatedParams(content.getDocuments().size())));
            LOGGER.debug("Message sent.");
        });

        LOGGER.debug("Sending request message...");
        clientManager.getBotLordClient().sendMessage(director, localizationLoader
                .localize(Localizations.Service.LOCALIZATION_FILES_REQUEST, director));
        LOGGER.debug("Message sent.");
    }
}
