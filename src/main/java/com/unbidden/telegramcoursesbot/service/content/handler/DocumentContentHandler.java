package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.ContentTextData;
import com.unbidden.telegramcoursesbot.model.content.Document;
import com.unbidden.telegramcoursesbot.model.content.DocumentContent;
import com.unbidden.telegramcoursesbot.model.content.Photo;
import com.unbidden.telegramcoursesbot.repository.DocumentContentRepository;
import com.unbidden.telegramcoursesbot.repository.DocumentRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup.SendMediaGroupBuilder;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class DocumentContentHandler implements LocalizedContentHandler<DocumentContent> {
    private final PhotoRepository photoRepository;

    private final DocumentRepository documentRepository;

    private final DocumentContentRepository documentContentRepository;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public DocumentContent parseLocalized(@NonNull List<Message> messages, boolean isLocalized) {
        final List<Document> documents = new ArrayList<>();
        String captions = null;

        for (Message message : messages) {
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
            if (message.getCaption() != null && !message.getCaption().isEmpty()) {
                captions = message.getCaption();
            }
            documents.add(document);
        }
        documentRepository.saveAll(documents);
        DocumentContent documentContent = new DocumentContent();
        if (captions != null) {
            documentContent.setData(new ContentTextData(captions, isLocalized));
        }
        documentContent.setDocuments(documents);
        return documentContent;
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user) {
        return sendContent(content, user, false);
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user, boolean isProtected) {
        final List<InputMedia> inputMedias = new ArrayList<>();
        final DocumentContent documentContent = (DocumentContent)content;
        final SendMediaGroupBuilder builder = SendMediaGroup.builder()
                .chatId(user.getId())
                .protectContent(isProtected);

        Localization captionsLoc = null;
        if (documentContent.getData() != null) {
            if (documentContent.getData().isLocalization()) {
                captionsLoc = localizationLoader.getLocalizationForUser(
                        documentContent.getData().getData(), user);
            } else {
                captionsLoc = new Localization(documentContent.getData().getData());
            }
        }
        for (Document document : documentContent.getDocuments()) {
            final InputMediaDocument inputMedia = new InputMediaDocument(document.getId());
            if (document.getThumbnail() != null) {
                inputMedia.setThumbnail(new InputFile(document.getThumbnail().getId()));
            }
            if (captionsLoc != null) {
                inputMedia.setCaption(captionsLoc.getData());
                inputMedia.setCaptionEntities(captionsLoc.getEntities());
            }
            inputMedias.add(inputMedia);
        }
        builder.medias(inputMedias);
        try {
            return bot.execute(builder.build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send documents media group in content "
                    + content.getId() + " to user " + user.getId(), e);
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
}
