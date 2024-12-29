package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.LocalizationDao;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.Document;
import com.unbidden.telegramcoursesbot.model.content.DocumentContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class LocalizationFileUploadButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            LocalizationFileUploadButtonHandler.class);

    private static final String FILES_UPDATED = "${filesUpdated}";

    private static final String SERVICE_LOCALIZATION_FILES_REQUEST =
            "service_localization_files_request";
    private static final String SERVICE_LOCALIZATION_FILES_UPDATED =
            "service_localization_files_updated";

    private static final String ERROR_DOWNLOAD_FILE = "error_download_file";
    private static final String ERROR_NO_LANGUAGE_CODE = "error_no_language_code";
    private static final String ERROR_LANGUAGE_CODE_LENGTH = "error_language_code_length";

    private final LocalizationDao localizationDao;

    private final ContentSessionService sessionService;

    private final BotService botService;

    private final ContentService contentService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);
        
        sessionService.createSession(user, bot, m -> {
            LOGGER.info("User " + user.getId() + " is trying to update application "
                    + "localizations files.");

            final Message lastMessage = m.getLast();
            final String languageCode;
            if (lastMessage.hasText()) {
                if (lastMessage.getText().length() > 3
                        || lastMessage.getText().length() < 2) {
                    throw new InvalidDataSentException("Language code must be "
                            + "between 2 and 3 characters", localizationLoader
                            .getLocalizationForUser(ERROR_LANGUAGE_CODE_LENGTH, user));
                }
                languageCode = lastMessage.getText();
                LOGGER.debug("Localization files will be added for language code "
                        + languageCode + ".");
            } else {
                throw new InvalidDataSentException("Threre must be a language code for which the "
                        + "localization files will be applied", localizationLoader
                        .getLocalizationForUser(ERROR_NO_LANGUAGE_CODE, user));
            }

            final DocumentContent content = (DocumentContent)contentService
                    .parseAndPersistContent(bot, m, List.of(MediaType.DOCUMENT));
            LOGGER.debug("Content " + content.getId() + " will be used for localization files. "
                    + "There are " + content.getDocuments().size() + " documents present.");
            
            localizationDao.createLanguageSubDir(languageCode);
            for (Document document : content.getDocuments()) {
                textUtil.checkIfDocumentIsALocalization(document, user, localizationLoader);
                try {
                    final File file = clientManager.getBotFatherClient()
                            .execute(new GetFile(document.getId()));
                    final Path path = localizationDao.addOrUpdateLocalizationsFile(
                            clientManager.getBotFatherClient().downloadFileAsStream(file),
                            document.getFileName(), languageCode);

                    LOGGER.info("Localization file " + path.toString() + " has been updated.");
                } catch (TelegramApiException e) {
                    throw new TelegramException("Unable to download file " + document.getId(),
                            localizationLoader.getLocalizationForUser(ERROR_DOWNLOAD_FILE,
                            user), e);
                }
            }
            LOGGER.info(content.getDocuments().size() + " localization files have been updated.");
            LOGGER.debug("Sending confirmation message...");
            clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_LOCALIZATION_FILES_UPDATED, user,
                    FILES_UPDATED, content.getDocuments().size()));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending request message...");
        clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_LOCALIZATION_FILES_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}
