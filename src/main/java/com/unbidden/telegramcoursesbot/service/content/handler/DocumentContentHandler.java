package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.Document;
import com.unbidden.telegramcoursesbot.model.content.DocumentContent;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import com.unbidden.telegramcoursesbot.model.content.Photo;
import com.unbidden.telegramcoursesbot.repository.DocumentContentRepository;
import com.unbidden.telegramcoursesbot.repository.DocumentRepository;
import com.unbidden.telegramcoursesbot.repository.MarkerAreaRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public class DocumentContentHandler extends AbstractContentHandler<DocumentContent> {
    private static final Logger LOGGER = LogManager.getLogger(DocumentContentHandler.class);

    private final PhotoRepository photoRepository;

    private final DocumentRepository documentRepository;

    private final DocumentContentRepository documentContentRepository;

    private final MarkerAreaRepository markerAreaRepository;

    private final TextContentHandler textContentHandler;

    private final ClientManager clientManager;

    public DocumentContentHandler(LocalizationLoader localizationLoader, PhotoRepository photoRepository,
            DocumentRepository documentRepository, DocumentContentRepository documentContentRepository,
            MarkerAreaRepository markerAreaRepository, TextContentHandler textContentHandler,
            ClientManager clientManager) {
        super(localizationLoader);
        this.photoRepository = photoRepository;
        this.documentRepository = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.markerAreaRepository = markerAreaRepository;
        this.textContentHandler = textContentHandler;
        this.clientManager = clientManager;
    }

    @Override
    @Transactional
    public DocumentContent parseAndPersist(Bot bot, List<Message> messages, String languageCode, boolean isProtected) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        
        final DocumentContent documentContent = new DocumentContent();
        final List<Document> documents = new ArrayList<>();
        final List<MarkerArea> markers = new ArrayList<>();

        String captions = null;
        for (final Message message : messages) {
            if (captions == null && message.hasText()) {
                captions = message.getText();
                if (message.getEntities() != null) {
                    markers.addAll(message.getEntities().stream()
                            .map(e -> new MarkerArea(e, documentContent)).toList());
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
            if (captions == null && message.getCaption() != null && !message.getCaption().isBlank()) {
                captions = message.getCaption();
                if (message.getCaptionEntities() != null) {
                    markers.addAll(message.getCaptionEntities().stream()
                        .map(e -> new MarkerArea(e, documentContent)).toList());
                }
            }
        }
        documentRepository.saveAll(documents);
        documentContent.setBot(bot);
        documentContent.setData(captions);
        documentContent.setDocuments(documents);
        documentContent.setLanguageCode(languageCode);
        documentContent.setType(getContentType());
        documentContent.setProtected(isProtected);
        documentContentRepository.save(documentContent);
        markerAreaRepository.saveAll(markers);
        
        return documentContent;
    }
    
    @Override
    public List<CompletableFuture<List<Message>>> sendContentInBulkAsync(List<Long> userIds, Bot bot, LocalizedContent content) {
        Assert.notNull(userIds, "userIds cannot be null");
        Assert.notEmpty(userIds, "userIds cannot be empty");
        Assert.noNullElements(userIds, "userIds cannot contain null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");

        final List<InputMedia> inputMedias = new ArrayList<>();
        final DocumentContent documentContent = (DocumentContent)content;
        documentContent.setDocuments(documentRepository.findByContent(documentContent.getId()));
        
        for (final Document document : documentContent.getDocuments()) {
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
            return textContentHandler.sendContentInBulkAsync(userIds, bot, content);
        }

        if (content.getData() != null) {
            inputMedias.get(inputMedias.size() - 1).setCaption(content.getData());
            inputMedias.get(inputMedias.size() - 1).setCaptionEntities(markerAreaRepository.findByContentId(
                    content.getId()).stream().map(MarkerArea::toMessageEntity).toList());
        }

        final var client = clientManager.getClient(bot);

        if (inputMedias.size() == 1) {
            final InputMedia inputMedia = inputMedias.get(0);
            final InputFile inputFile = new InputFile(inputMedia.getMedia());

            LOGGER.debug("Document content " + content.getId() + " contains only one media.");
            return userIds.stream().map(id -> client.executeAsync(SendDocument.builder()
                    .chatId(id)
                    .protectContent(content.isProtected())
                    .document(inputFile)
                    .caption(inputMedia.getCaption())
                    .captionEntities(inputMedia.getCaptionEntities())
                    .build()).thenApply(r -> List.of(r))).toList();
        }

        return userIds.stream().map(id -> client.executeAsync(SendMediaGroup.builder()
                .chatId(id)
                .protectContent(content.isProtected())
                .medias(inputMedias)
                .build())).toList();
    }
    
    @Override
    public MediaType getContentType() {
        return MediaType.DOCUMENT;
    }
}
