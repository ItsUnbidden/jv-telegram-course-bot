package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.ContentTextData;
import com.unbidden.telegramcoursesbot.model.content.Document;
import com.unbidden.telegramcoursesbot.model.content.DocumentContent;
import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import com.unbidden.telegramcoursesbot.model.content.Photo;
import com.unbidden.telegramcoursesbot.repository.DocumentContentRepository;
import com.unbidden.telegramcoursesbot.repository.DocumentRepository;
import com.unbidden.telegramcoursesbot.repository.MarkerAreaRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class DocumentContentHandler implements LocalizedContentHandler<DocumentContent> {
    private static final Logger LOGGER = LogManager.getLogger(DocumentContentHandler.class);

    private static final String ERROR_SEND_CONTENT_FAILURE = "error_send_content_failure";

    private final PhotoRepository photoRepository;

    private final DocumentRepository documentRepository;

    private final DocumentContentRepository documentContentRepository;

    private final MarkerAreaRepository markerAreaRepository;

    private final LocalizationLoader localizationLoader;

    private final TextContentHandler textContentHandler;

    private final UserService userService;

    private final ClientManager clientManager;

    @Override
    public DocumentContent parseLocalized(@NonNull List<Message> messages, @NonNull Bot bot,
            boolean isLocalized) {
        return parse0(messages, bot, null, isLocalized);
    }

    @Override
    public DocumentContent parseLocalized(@NonNull List<Message> messages, @NonNull Bot bot,
            @NonNull String localizationName, @NonNull String languageCode) {
        final DocumentContent content = parse0(messages, bot, localizationName, true);
        content.setLanguageCode(languageCode);
        return content;
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            @NonNull Bot bot) {
        return sendContent(content, user, bot, false, false);
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            @NonNull Bot bot, boolean isProtected, boolean skipText) {
        final List<InputMedia> inputMedias = new ArrayList<>();
        final DocumentContent documentContent = (DocumentContent)content;

        Localization captionsLoc = null;
        if (documentContent.getData() != null && !skipText) {
            if (documentContent.getData().isLocalization()) {
                captionsLoc = localizationLoader.getLocalizationForUser(
                        documentContent.getData().getData(), user);
            } else {
                captionsLoc = new Localization(documentContent.getData().getData());
                captionsLoc.setEntities(documentContent.getData().getEntities().stream()
                        .map(m -> m.toMessageEntity()).toList());
            }
        }
        for (Document document : documentContent.getDocuments()) {
            final InputMediaDocument inputMedia = new InputMediaDocument(document.getId());
            if (document.getThumbnail() != null) {
                inputMedia.setThumbnail(new InputFile(document.getThumbnail().getId()));
            }
            inputMedias.add(inputMedia);
        }

        if (inputMedias.isEmpty()) {
            LOGGER.warn("Content " + content.getId() + " is of type " + content.getType()
                    + " but does not have any relevant content. Text content handler "
                    + "will be used instead.");
            return textContentHandler.sendContent(content, user, bot, isProtected, false);
        }

        if (captionsLoc != null) {
            inputMedias.get(inputMedias.size() - 1).setCaption(captionsLoc.getData());
            inputMedias.get(inputMedias.size() - 1).setCaptionEntities(captionsLoc.getEntities());
        }

        if (inputMedias.size() == 1) {
            final InputMedia inputMedia = inputMedias.get(0);
            LOGGER.debug("Document content " + content.getId() + " contains only one media.");

            if (inputMedia.getClass().equals(InputMediaDocument.class)) {
                try {
                    return List.of(clientManager.getClient(bot).execute(SendDocument.builder()
                            .chatId(user.getId())
                            .protectContent(isProtected)
                            .document(new InputFile(inputMedia.getMedia()))
                            .caption((captionsLoc != null) ? captionsLoc.getData() : null)
                            .captionEntities((captionsLoc != null)
                                ? captionsLoc.getEntities() : List.of())
                            .build()));
                } catch (TelegramApiException e) {
                    throw new TelegramException("Unable to send document media in content "
                            + content.getId() + " to user " + user.getId(), localizationLoader
                            .getLocalizationForUser(ERROR_SEND_CONTENT_FAILURE, user), e);
                }
            }
        }

        try {
            return clientManager.getClient(bot).execute(SendMediaGroup.builder()
                    .chatId(user.getId())
                    .protectContent(isProtected)
                    .medias(inputMedias)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send documents media group in content "
                    + content.getId() + " to user " + user.getId(), localizationLoader
                    .getLocalizationForUser(ERROR_SEND_CONTENT_FAILURE, user), e);
        }
    }

    @Override
    @NonNull
    public Optional<DocumentContent> findById(@NonNull Long id) {
        return documentContentRepository.findById(id);
    }

    @Override
    @NonNull
    public DocumentContent persist(@NonNull Content content) {
        final DocumentContent documentContent = (DocumentContent)content;
        return documentContentRepository.save(documentContent);
    }
    
    @Override
    @NonNull
    public MediaType getContentType() {
        return MediaType.DOCUMENT;
    }

    private DocumentContent parse0(List<Message> messages, Bot bot, String localizationName,
            boolean isLocalized) {
        final List<Document> documents = new ArrayList<>();
        final List<MarkerArea> markers = new ArrayList<>();
        boolean isTextSetUp = localizationName != null;
        String languageCode = null;
        String captions = localizationName;

        for (Message message : messages) {
            if (!isTextSetUp && message.hasText()) {
                isTextSetUp = true;
                captions = message.getText();
                if (message.getEntities() != null
                        && !message.getEntities().isEmpty()) {
                    markers.addAll(message.getEntities().stream()
                        .map(e -> markerAreaRepository.save(new MarkerArea(e))).toList());
                }
                continue;
            }
            if (message.hasDocument()) {
                final Document document = new Document(message.getDocument());
                if (message.getDocument().getThumbnail() != null) {
                    final Optional<Photo> potentialThumbnail = photoRepository.findById(
                            message.getDocument().getThumbnail().getFileUniqueId());
                    if (potentialThumbnail.isPresent()) {
                        document.setThumbnail(potentialThumbnail.get());
                    } else {
                        document.setThumbnail(photoRepository.save(
                                new Photo(message.getDocument().getThumbnail())));
                    }
                }
                documents.add(document);
            }
            if (!isTextSetUp && message.getCaption() != null && !message.getCaption().isEmpty()) {
                captions = message.getCaption();
                if (message.getCaptionEntities() != null
                        && !message.getCaptionEntities().isEmpty()) {
                    markers.addAll(message.getCaptionEntities().stream()
                        .map(e -> markerAreaRepository.save(new MarkerArea(e))).toList());
                }
            }
            if (languageCode == null) {
                languageCode = userService.getUser(message.getFrom().getId(),
                        userService.getDiretor()).getLanguageCode();
            }
        }
        documentRepository.saveAll(documents);
        DocumentContent documentContent = new DocumentContent();
        documentContent.setBot(bot);
        documentContent.setData(new ContentTextData(captions, markers, isLocalized));
        documentContent.setDocuments(documents);
        documentContent.setLanguageCode(languageCode);
        
        return documentContent;
    }
}
