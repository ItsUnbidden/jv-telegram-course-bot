package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {
    private static final Logger LOGGER = LogManager.getLogger(ContentServiceImpl.class);

    private static final String MEDIA_TYPES_DIVIDER = " ";

    private final ContentMappingRepository contentMappingRepository;

    private final ContentManager contentManager;

    private final TextUtil textUtil;

    @Value("${telegram.bot.message.language.priority}")
    private String languagePriorityStr;

    @Override
    @NonNull
    public LocalizedContent parseAndPersistContent(@NonNull List<Message> messages) {
        return parseAndPersistContent(messages, List.of());
    }

    @Override
    @NonNull
    public LocalizedContent parseAndPersistContent(@NonNull List<Message> messages, boolean isLocalized) {
        return persistContent(null, messages, List.of(), isLocalized);
    }

    @Override
    @NonNull
    public LocalizedContent parseAndPersistContent(@NonNull List<Message> messages,
            @NonNull List<MediaType> allowedContentTypes) {
        return persistContent(null, messages, allowedContentTypes, false);
    }

    @Override
    @NonNull
    public LocalizedContent parseAndPersistContent(@NonNull List<Message> messages,
            @NonNull String localizationName, @NonNull String languageCode) {
        LOGGER.info("Initiating parsing of a message to content with predefined localization...");
        final MediaType messagesContentType = defineContentType(messages);

        final LocalizedContentHandler<? extends Content> handler =
                contentManager.getHandler(messagesContentType);

        final Content content = handler.parseLocalized(messages, localizationName, languageCode);
        content.setType(messagesContentType);

        return (LocalizedContent)handler.persist(content);
    }

    @Override
    @NonNull
    @Deprecated
    public LocalizedContent parseAndUpdateContent(@NonNull Long contentId,
            @NonNull List<Message> messages) {
        return persistContent(contentId, messages, List.of(), false);
    }

    @Override
    @NonNull
    @Deprecated
    public LocalizedContent parseAndUpdateContent(@NonNull Long contentId, @NonNull List<Message> messages,
            boolean isLocalized) {
        return persistContent(contentId, messages, List.of(), isLocalized);
    }

    @Override
    @NonNull
    @Deprecated
    public LocalizedContent parseAndUpdateContent(@NonNull Long contentId, @NonNull List<Message> messages,
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
    public List<Message> sendLocalizedContent(@NonNull ContentMapping contentMapping,
            @NonNull UserEntity user) {
        Assert.notEmpty(contentMapping.getContent(), "Content mapping cannot be empty");
        final Map<String, LocalizedContent> contentMap =
                getContentMap(contentMapping.getContent());

        if (contentMap.containsKey(user.getLanguageCode())) {
            LOGGER.debug("Localized content in group " + contentMapping.getId()
                    + " for user " + user.getId() + "'s prefered code " + user.getLanguageCode()
                    + " is available.");
            return sendContent(getById(contentMap.get(user.getLanguageCode()).getId()), user);
        }
        LOGGER.debug("Localized content in group " + contentMapping.getId() + " for user "
                + user.getId() + "'s prefered code " + user.getLanguageCode()
                + " is not available. Looking over the language code priority list...");
        final String[] languagePriority = textUtil.getLanguagePriority();

        for (String code : languagePriority) {
            if (!code.equals(user.getLanguageCode())) {
                if (contentMap.containsKey(code)) {
                    LOGGER.debug("Localized content in group " + contentMapping.getId()
                            + " has been found for language code " + code + ".");
                    return sendContent(getById(contentMap.get(code).getId()), user);
                }
            }
        }
        final LocalizedContent firstAvailableContent = contentMapping.getContent().get(0);
        LOGGER.warn("There is no available content in group " + contentMapping.getId()
                + " for any of the priority language codes. First content in the list (Id: "
                + firstAvailableContent.getId() + ") will be used instead.");
        return sendContent(getById(firstAvailableContent.getId()), user);
    }

    @Override
    @NonNull
    public LocalizedContent getById(@NonNull Long id) {
        return contentManager.getById(id);
    }

    @Override
    @NonNull
    public ContentMapping getMappingById(@NonNull Long id) {
        return contentMappingRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Content mapping with id " + id
                + " does not exist"));
    }

    @Override
    @NonNull
    public List<MediaType> parseMediaTypes(@NonNull String mediaTypesStr) {
        final List<MediaType> mediaTypes = new ArrayList<>();

        if (!mediaTypesStr.isBlank()) {
            final String[] mediaTypesStrArray = mediaTypesStr.split(MEDIA_TYPES_DIVIDER);

            for (String mediaTypeStr : mediaTypesStrArray) {
                mediaTypes.add(MediaType.valueOf(mediaTypeStr));
            }
        }
        return mediaTypes;
    }

    private MediaType defineContentType(List<Message> messages) {
        LOGGER.info("Trying to define content type of messages...");

        int numberOfText = 0;
        int numberOfAudio = 0;
        int numberOfDocuments = 0;
        int numberOfGraphics = 0;
        boolean isCaptionPresent = false;
        for (Message message : messages) {
            if (message.hasText()) {
                numberOfText++;
            }
            if (message.hasAudio()) {
                numberOfAudio++;
            }
            if (message.hasDocument()) {
                numberOfDocuments++;
            }
            if (message.hasVideo() || message.hasPhoto()) {
                numberOfGraphics++;
            }
            if (message.getCaption() != null && !message.getCaption().isEmpty()) {
                isCaptionPresent = true;
            }
        }

        if (numberOfText != 0 && isCaptionPresent) {
            throw new InvalidDataSentException("Captions and text in the "
                    + "same content are not supported");
        }
        if (numberOfText > 1) {
            throw new InvalidDataSentException("Several text messages are not supported");
        }

        if (numberOfAudio != 0) {
            if (messages.size() != numberOfAudio + numberOfText) {
                throw new InvalidDataSentException("Audio files can only be "
                        + "grouped with other audio files and text");
            }
            LOGGER.info("Content type is audio.");
            return MediaType.AUDIO;
        }
        
        if (numberOfDocuments != 0) {
            if (messages.size() != numberOfDocuments + numberOfText) {
                throw new InvalidDataSentException("Documents can only be "
                        + "grouped with other documents and text");
            }
            LOGGER.info("Content type is document.");
            return MediaType.DOCUMENT;
        }

        if (numberOfGraphics != 0) {
            if (messages.size() != numberOfGraphics + numberOfText) {
                throw new InvalidDataSentException("Videos and photos cannot be grouped "
                        + "with other media types");
            }
            LOGGER.info("Content type is graphic.");
            return MediaType.GRAPHICS;
        }

        LOGGER.info("Content type is text.");
        return MediaType.TEXT;
    }

    private LocalizedContent persistContent(Long contentId, List<Message> messages,
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
        content.setType(messagesContentType);
        content.setId(contentId);

        return (LocalizedContent)handler.persist(content);
    }

    private Map<String, LocalizedContent> getContentMap(List<LocalizedContent> content) {
        final Map<String, LocalizedContent> contentMap = new HashMap<>();

        for (LocalizedContent localizedContent : content) {
            contentMap.put(localizedContent.getLanguageCode(), localizedContent);
        }
        return contentMap;
    }

    @Override
    @NonNull
    public ContentMapping addNewLocalization(@NonNull Long mappingId, @NonNull LocalizedContent content) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addNewLocalization'");
    }

    @Override
    @NonNull
    public ContentMapping removeLocalization(@NonNull Long mappingId, @NonNull Long contentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeLocalization'");
    }
}
