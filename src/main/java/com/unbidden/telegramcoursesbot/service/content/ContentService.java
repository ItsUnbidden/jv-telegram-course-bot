package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.LocalizedContentRepository;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class ContentService {
    private static final Logger LOGGER = LogManager.getLogger(ContentService.class);

    private final LocalizedContentRepository localizedContentRepository;

    private final ContentHandlerManager contentManager;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Value("${telegram.bot.message.language.priority}")
    private String languagePriorityStr;

    @Transactional
    public LocalizedContent parseAndPersistContent(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        return parseAndPersistContent(botRole, messages, botRole.getUser().getLanguageCode());
    }

    @Transactional
    public LocalizedContent parseAndPersistContent(BotRole botRole,
            List<Message> messages, String languageCode) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        return parseAndPersistContent0(botRole, messages, List.of(), languageCode);
    }

    @Transactional
    public LocalizedContent parseAndPersistContent(BotRole botRole,
            List<Message> messages, List<MediaType> allowedContentTypes) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");
        Assert.notNull(allowedContentTypes, "allowedContentTypes cannot be null");

        return parseAndPersistContent0(botRole, messages, allowedContentTypes, botRole.getUser().getLanguageCode());
    }

    @Transactional
    public LocalizedContent parseAndPersistContent(BotRole botRole,
            List<Message> messages, String languageCode, List<MediaType> allowedContentTypes) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");
        Assert.notNull(allowedContentTypes, "allowedContentTypes cannot be null");

        return parseAndPersistContent0(botRole, messages, allowedContentTypes, languageCode);
    }

    @Transactional
    public ContentMapping addNewLocalization(BotRole botRole, Long mappingId, String languageCode, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        final ContentMapping mapping = entityUtil.getMappingById(botRole, mappingId);

        if (mapping.getContent().stream().anyMatch(c -> c.getLanguageCode().equals(languageCode))) {
            throw new InvalidDataSentException("Localization with language code "
                    + languageCode + " is already present in mapping " + mapping.getId(),
                    localizationLoader.localize(Localizations.Error.LOCALIZED_CONTENT_IS_ALREADY_PRESENT, botRole,
                        new Localizations.Error.LocalizedContentIsAlreadyPresentParams(mapping.getId(), languageCode)));
        }

        mapping.getContent().add(parseAndPersistContent(botRole, messages, languageCode));

        return mapping;
    }

    @Transactional
    public boolean removeLocalization(BotRole botRole, Long mappingId, String languageCode) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");

        final ContentMapping mapping = entityUtil.getMappingById(botRole, mappingId);

        if (mapping.getContent().size() < 2) {
            throw new ForbiddenOperationException("Mapping must always contain at least one localized content.",
                    localizationLoader.localize(Localizations.Error.MAPPING_ONE_CONTENT, botRole));
        }

        LocalizedContent contentForDeletion = null;
        for (final LocalizedContent content : mapping.getContent()) {
            if (content.getLanguageCode().equals(languageCode)) {
                contentForDeletion = content;
                localizedContentRepository.delete(contentForDeletion);
                break;
            }
        }
        
        if (contentForDeletion == null) return false;
        
        mapping.getContent().remove(contentForDeletion);
        return true;
    }

    private LocalizedContent parseAndPersistContent0(BotRole botRole, List<Message> messages,
            List<MediaType> allowedContentTypes, String languageCode) {
        LOGGER.debug("Initiating parsing of a message to content.");
        final MediaType messagesContentType = defineContentType(botRole, messages);

        if (!allowedContentTypes.isEmpty()) {
            LOGGER.debug("Allowed content types are " + allowedContentTypes + ".");

            if (!allowedContentTypes.contains(messagesContentType)) {
                throw new InvalidDataSentException("Allowed content types are " + allowedContentTypes + " but user sent messages of type "
                        + messagesContentType, localizationLoader.localize(Error.CONTENT_MEDIA_GROUP_DOES_NOT_MATCH, botRole,
                            new Error.ContentMediaGroupDoesNotMatchParams(messagesContentType, allowedContentTypes)));
            }
        }
        
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentManager
                    .getHandler(messagesContentType);

        return handler.parseAndPersist(botRole, messages, languageCode);
        } catch (NoImplementationException e) {
            throw new InvalidDataSentException("Unknown media type", localizationLoader
                    .localize(Error.UNKNOWN_MEDIA_TYPE, botRole));
        }
    }

    private MediaType defineContentType(BotRole botRole, List<Message> messages) {
        LOGGER.debug("Trying to define content type of messages...");

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
                    + "same content are not supported", localizationLoader.localize(
                    Error.CONTENT_TEXT_AND_CAPTIONS, botRole));
        }
        if (numberOfText > 1) { // TODO: there is a limit on how large a single Telegram message can be. If a user sends a message that is too large, Telegram will cut it in two or more pieces. Make sure this behavior is accomodated for.
            throw new InvalidDataSentException("Several text messages are not supported",
                    localizationLoader.localize(Error.CONTENT_SEVERAL_TEXT, botRole));
        }

        if (numberOfAudio != 0) {
            if (messages.size() != numberOfAudio + numberOfText) {
                throw new InvalidDataSentException("Audio files can only be "
                        + "grouped with other audio files and text", localizationLoader
                        .localize(Error.CONTENT_AUDIO_GROUP_FAILURE, botRole));
            }
            LOGGER.debug("Content type is audio.");
            return MediaType.AUDIO;
        }
        
        if (numberOfDocuments != 0) {
            if (messages.size() != numberOfDocuments + numberOfText) {
                throw new InvalidDataSentException("Documents can only be "
                        + "grouped with other documents and text", localizationLoader
                        .localize(Error.CONTENT_DOCUMENT_GROUP_FAILURE, botRole));
            }
            LOGGER.debug("Content type is document.");
            return MediaType.DOCUMENT;
        }

        if (numberOfGraphics != 0) {
            if (messages.size() != numberOfGraphics + numberOfText) {
                throw new InvalidDataSentException("Videos and photos cannot be grouped "
                        + "with other media types", localizationLoader
                        .localize(Error.CONTENT_GRAPHICS_GROUP_FAILURE, botRole));
            }
            LOGGER.debug("Content type is graphics.");
            return MediaType.GRAPHICS;
        }

        LOGGER.debug("Content type is text.");
        return MediaType.TEXT;
    }
}
