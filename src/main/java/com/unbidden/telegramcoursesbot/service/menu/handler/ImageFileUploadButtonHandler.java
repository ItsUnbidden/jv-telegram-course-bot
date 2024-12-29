package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.ImageDao;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.Document;
import com.unbidden.telegramcoursesbot.model.content.DocumentContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class ImageFileUploadButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            ImageFileUploadButtonHandler.class);

    private static final String EXPECTED_MESSAGES_AMOUNT = "${expectedMessagesAmount}";
    private static final String PROVIDED_MESSAGES_NUMBER = "${providedMessagesNumber}";

    private static final String SERVICE_INVOICE_IMAGE_REQUEST = "service_invoice_image_request";
    private static final String SERVICE_INVOICE_IMAGE_UPDATED = "service_invoice_image_updated";

    private static final String ERROR_AMOUNT_OF_MESSAGES = "error_amount_of_messages";
    private static final String ERROR_NO_COURSE_NAME = "error_no_course_name";
    private static final String ERROR_DOWNLOAD_FILE = "error_download_file";

    private static final int EXPECTED_MESSAGES = 2;

    private final ImageDao imageDao;

    private final ContentSessionService sessionService;

    private final CourseService courseService;

    private final BotService botService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);
        
        sessionService.createSession(user, bot, m -> {
            LOGGER.info("User " + user.getId() + " is trying to update a course invoice image.");

            if (m.size() != EXPECTED_MESSAGES) {
                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PROVIDED_MESSAGES_NUMBER, m.size());
                parameterMap.put(EXPECTED_MESSAGES_AMOUNT, EXPECTED_MESSAGES);
                throw new InvalidDataSentException("2 messages were expected, where the first is "
                        + "the image and the second one is the course name", localizationLoader
                        .getLocalizationForUser(ERROR_AMOUNT_OF_MESSAGES, user, parameterMap));
            }

            final Message lastMessage = m.getLast();
            final Course course;
            if (lastMessage.hasText()) {
                course = courseService.getCourseByName(lastMessage.getText(), user, bot);
                LOGGER.debug("Invoice image will be added for course " + course.getName() + ".");
            } else {
                throw new InvalidDataSentException("There must be a course name to identify the "
                        + "course for the invoice of which the image will be used.",
                        localizationLoader.getLocalizationForUser(ERROR_NO_COURSE_NAME, user));
            }

            final DocumentContent content = (DocumentContent)contentService
                    .parseAndPersistContent(bot, m, List.of(MediaType.DOCUMENT));
            LOGGER.debug("Content " + content.getId() + " will be used for the invoice image "
                    + "for course " + course.getName() + ".");
            
            final Document document = content.getDocuments().get(0);
            try {
                final File file = clientManager.getBotFatherClient()
                        .execute(new GetFile(document.getId()));
                final Path path = imageDao.addOrUpdateImage(clientManager.getBotFatherClient()
                        .downloadFileAsStream(file), course.getName());

                LOGGER.info("Invoice image file " + path.toString() + " has been updated.");
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to download file " + document.getId(),
                        localizationLoader.getLocalizationForUser(ERROR_DOWNLOAD_FILE,
                        user), e);
            }
            
            LOGGER.info("Invoice image file for course " + course.getName()
                    + " has been updated.");
            LOGGER.debug("Sending confirmation message...");
            clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_INVOICE_IMAGE_UPDATED, user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending request message...");
        clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_INVOICE_IMAGE_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}
