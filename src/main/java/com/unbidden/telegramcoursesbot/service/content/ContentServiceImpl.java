package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {
    private static final Logger LOGGER = LogManager.getLogger(ContentServiceImpl.class);

    private static final String MEDIA_TYPES_DIVIDER = " ";

    private final ContentManager contentManager;

    @Override
    @NonNull
    public Content parseAndPersistContent(@NonNull List<Message> messages) {
        return parseAndPersistContent(messages, List.of());
    }

    @Override
    @NonNull
    public Content parseAndPersistContent(@NonNull List<Message> messages, boolean isLocalized) {
        return persistContent(null, messages, List.of(), isLocalized);
    }

    @Override
    @NonNull
    public Content parseAndPersistContent(@NonNull List<Message> messages,
            @NonNull List<MediaType> allowedContentTypes) {
        return persistContent(null, messages, allowedContentTypes, false);
    }

    @Override
    @NonNull
    public Content parseAndUpdateContent(@NonNull Long contentId,
            @NonNull List<Message> messages) {
        return persistContent(contentId, messages, List.of(), false);
    }

    @Override
    @NonNull
    public Content parseAndUpdateContent(@NonNull Long contentId, @NonNull List<Message> messages,
            boolean isLocalized) {
        return persistContent(contentId, messages, List.of(), isLocalized);
    }

    @Override
    @NonNull
    public Content parseAndUpdateContent(@NonNull Long contentId, @NonNull List<Message> messages,
            @NonNull List<MediaType> allowedContentTypes) {
        return persistContent(contentId, messages, allowedContentTypes, false);
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user) {
        return contentManager.getHandler(content.getType()).sendContent(content, user);
    }

    @Override
    @NonNull
    public Content getById(@NonNull Long id) {
        return contentManager.getById(id);
    }

    @Override
    @NonNull
    public List<MediaType> parseMediaTypes(@NonNull String mediaTypesStr) {
        final String[] mediaTypesStrArray = mediaTypesStr.split(MEDIA_TYPES_DIVIDER);
        final List<MediaType> mediaTypes = new ArrayList<>();

        for (String mediaTypeStr : mediaTypesStrArray) {
            mediaTypes.add(MediaType.valueOf(mediaTypeStr));
        }
        return mediaTypes;
    }

    private MediaType defineContentType(List<Message> messages) {
        LOGGER.info("Trying to define content type of messages...");
        final int numberOfAudio = messages.stream().filter(m -> m.hasAudio()).toList().size();

        if (numberOfAudio != 0) {
            if (messages.size() != numberOfAudio) {
                throw new InvalidDataSentException("Audio files can only be "
                        + "grouped with other audio files");
            }
            LOGGER.info("Content type is audio.");
            return MediaType.AUDIO;
        }
        final int numberOfDocuments = messages.stream().filter(m -> m.hasDocument())
                .toList().size();
        
        if (numberOfDocuments != 0) {
            if (messages.size() != numberOfDocuments) {
                throw new InvalidDataSentException("Documents can only be "
                        + "grouped with other documents");
            }
            LOGGER.info("Content type is document.");
            return MediaType.DOCUMENT;
        }
        final int numberOfGraphics = messages.stream().filter(m -> m.hasVideo() || m.hasPhoto())
                .toList().size();

        if (numberOfGraphics != 0) {
            if (messages.size() != numberOfGraphics) {
                throw new InvalidDataSentException("Videos and photos cannot be grouped "
                        + "with text messages, only with captions");
            }
            LOGGER.info("Content type is graphic.");
            return MediaType.GRAPHICS;
        }

        if (messages.size() != 1) {
            throw new InvalidDataSentException("Several text messages are not supported");
        }
        LOGGER.info("Content type is text.");
        return MediaType.TEXT;
    }

    private Content persistContent(Long contentId, List<Message> messages,
            List<MediaType> allowedContentTypes,
            boolean isLocalized) {
        LOGGER.info("Initiating parsing of a message to content.");
        final MediaType messagesContentType = defineContentType(messages);

        if (!allowedContentTypes.isEmpty()) {
            LOGGER.info("Allowed content types are " + allowedContentTypes + ".");

            if (!allowedContentTypes.contains(messagesContentType)) {
                throw new InvalidDataSentException("Allowed content types are "
                        + allowedContentTypes + " but user sent messages of type "
                        + messagesContentType);
            }
        }
        final LocalizedContentHandler<? extends Content> handler =
                contentManager.getHandler(messagesContentType);

        final Content content = (isLocalized) ? handler.parseLocalized(messages, isLocalized)
                : handler.parse(messages);
        content.setId(contentId);
        return handler.persist(content);
    }
}
