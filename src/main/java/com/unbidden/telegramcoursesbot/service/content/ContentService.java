package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.LocalizedException;
import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.LocalizedContentRepository;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class ContentService {
    private static final Logger LOGGER = LogManager.getLogger(ContentService.class);

    private static final String MEDIA_TYPES_DIVIDER = " ";

    private final LocalizedContentRepository localizedContentRepository;

    private final ContentHandlerManager contentManager;

    private final LocalizationLoader localizationLoader;

    private final TextUtil textUtil;

    private final EntityUtil entityUtil;

    @Value("${telegram.bot.message.language.priority}")
    private String languagePriorityStr;

    @Transactional
    public LocalizedContent parseAndPersistContent(UserEntity user, Bot bot, List<Message> messages) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty");

        return parseAndPersistContent(user, bot, messages, user.getLanguageCode());
    }

    @Transactional
    public LocalizedContent parseAndPersistContent(UserEntity user, Bot bot,
            List<Message> messages, String languageCode) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty");

        return parseAndPersistContent0(user, bot, messages, List.of(), languageCode);
    }

    @Transactional
    public LocalizedContent parseAndPersistContent(UserEntity user, Bot bot,
            List<Message> messages, List<MediaType> allowedContentTypes) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty");
        Assert.notNull(allowedContentTypes, "allowedContentTypes cannot be null");

        return parseAndPersistContent0(user, bot, messages, allowedContentTypes, user.getLanguageCode());
    }

    public List<Message> sendContent(UserEntity user, Bot bot, Long contentId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        final LocalizedContent content = entityUtil.getLocalizedContentById(user, bot, contentId);
        
        return sendContent(user, bot, content);
    }

    public List<Message> sendLocalizedContent(UserEntity user, Bot bot, Long mappingId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");

        return sendContent(user, bot, getLocalizedContentFromMapping(user, bot, mappingId));
    }

    public String getLocalizedText(UserEntity user, Bot bot, Long mappingId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");

        final LocalizedContent content = getLocalizedContentFromMapping(user, bot, mappingId);

        return content.getData();
    }

    private LocalizedContent getLocalizedContentFromMapping(UserEntity user, Bot bot, Long mappingId) {
        final ContentMapping mapping = entityUtil.getMappingById(user, bot, mappingId);
        final Map<String, LocalizedContent> contentMap = getContentMap(mapping.getContent());

        if (contentMap.containsKey(user.getLanguageCode())) {
            LOGGER.debug("Localized content in group " + mapping.getId()
                    + " for user " + user.getId() + "'s prefered code " + user.getLanguageCode()
                    + " is available.");
            return entityUtil.getLocalizedContentById(user, bot, contentMap.get(user.getLanguageCode()).getId());
        }
        LOGGER.debug("Localized content in group " + mapping.getId() + " for user "
                + user.getId() + "'s prefered code " + user.getLanguageCode()
                + " is not available. Looking over the language code priority list...");
        final List<String> languagePriority = textUtil.getLanguagePriority();

        for (final String code : languagePriority) {
            if (!code.equals(user.getLanguageCode())) {
                if (contentMap.containsKey(code)) {
                    LOGGER.debug("Localized content in group " + mapping.getId()
                            + " has been found for language code " + code + ".");
                    return entityUtil.getLocalizedContentById(user, bot, contentMap.get(code).getId());
                }
            }
        }
        final LocalizedContent firstAvailableContent = mapping.getContent().get(0);
        LOGGER.warn("There is no available content in group " + mapping.getId()
                + " for any of the priority language codes. First content in the list (Id: "
                + firstAvailableContent.getId() + ") will be used instead.");

        return entityUtil.getLocalizedContentById(user, bot, mapping.getContent().get(0).getId());
    }

    public List<MediaType> parseMediaTypes(@Nullable String mediaTypesStr) {
        final List<MediaType> mediaTypes = new ArrayList<>();

        if (mediaTypesStr == null || mediaTypesStr.isBlank()) return mediaTypes;

        final String[] mediaTypesStrArray = mediaTypesStr.split(MEDIA_TYPES_DIVIDER);

        for (final String mediaTypeStr : mediaTypesStrArray) {
            mediaTypes.add(MediaType.valueOf(mediaTypeStr));
        }
        
        return mediaTypes;
    }

    @Transactional
    public ContentMapping addNewLocalization(UserEntity user, Bot bot, Long mappingId, Long contentId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        final ContentMapping mapping = entityUtil.getMappingById(user, bot, mappingId);

        mapping.getContent().add(entityUtil.getLocalizedContentReference(contentId));
        return mapping;
    }

    @Transactional
    public boolean removeLocalization(UserEntity user, Bot bot, Long mappingId, String languageCode) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");

        final ContentMapping mapping = entityUtil.getMappingById(user, bot, mappingId);

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

    private LocalizedContent parseAndPersistContent0(UserEntity user, Bot bot, List<Message> messages,
            List<MediaType> allowedContentTypes, String languageCode) {
        LOGGER.debug("Initiating parsing of a message to content.");
        final MediaType messagesContentType = defineContentType(messages);

        if (!allowedContentTypes.isEmpty()) {
            LOGGER.debug("Allowed content types are " + allowedContentTypes + ".");

            if (!allowedContentTypes.contains(messagesContentType)) {
                throw new InvalidDataSentException("Allowed content types are "
                        + allowedContentTypes + " but user sent messages of type "
                        + messagesContentType, localizationLoader
                        .getLocalizationForUser(Error.CONTENT_MEDIA_GROUP_DOES_NOT_MATCH, user,
                            new Error.ContentMediaGroupDoesNotMatchParams(messagesContentType, allowedContentTypes)));
            }
        }
        
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentManager
                    .getHandler(messagesContentType);

        return handler.parseAndPersist(bot, messages, languageCode);
        } catch (NoImplementationException e) {
            throw new InvalidDataSentException("Unknown media type", localizationLoader
                    .getLocalizationForUser(Error.UNKNOWN_MEDIA_TYPE, user));
        }
    }

    private List<Message> sendContent(UserEntity user, Bot bot, LocalizedContent content) {
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentManager.getHandler(content.getType());

            return handler.sendContent(user, bot, content);
        } catch (NoImplementationException e) {
            throw new LocalizedException("Unknown media type", localizationLoader
                    .getLocalizationForUser(Error.UNKNOWN_MEDIA_TYPE, user));
        }
    }


    private MediaType defineContentType(List<Message> messages) {
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
        final UserEntity user = entityUtil.getUser(messages.get(0).getFrom());

        if (numberOfText != 0 && isCaptionPresent) {
            throw new InvalidDataSentException("Captions and text in the "
                    + "same content are not supported", localizationLoader.getLocalizationForUser(
                    Error.CONTENT_TEXT_AND_CAPTIONS, user));
        }
        if (numberOfText > 1) { // TODO: there is a limit on how large a single Telegram message can be. If a user sends a message that is too large, Telegram will cut it in two or more pieces. Make sure this behavior is accomodated for.
            throw new InvalidDataSentException("Several text messages are not supported",
                    localizationLoader.getLocalizationForUser(Error.CONTENT_SEVERAL_TEXT, user));
        }

        if (numberOfAudio != 0) {
            if (messages.size() != numberOfAudio + numberOfText) {
                throw new InvalidDataSentException("Audio files can only be "
                        + "grouped with other audio files and text", localizationLoader
                        .getLocalizationForUser(Error.CONTENT_AUDIO_GROUP_FAILURE, user));
            }
            LOGGER.debug("Content type is audio.");
            return MediaType.AUDIO;
        }
        
        if (numberOfDocuments != 0) {
            if (messages.size() != numberOfDocuments + numberOfText) {
                throw new InvalidDataSentException("Documents can only be "
                        + "grouped with other documents and text", localizationLoader
                        .getLocalizationForUser(Error.CONTENT_DOCUMENT_GROUP_FAILURE, user));
            }
            LOGGER.debug("Content type is document.");
            return MediaType.DOCUMENT;
        }

        if (numberOfGraphics != 0) {
            if (messages.size() != numberOfGraphics + numberOfText) {
                throw new InvalidDataSentException("Videos and photos cannot be grouped "
                        + "with other media types", localizationLoader
                        .getLocalizationForUser(Error.CONTENT_GRAPHICS_GROUP_FAILURE, user));
            }
            LOGGER.debug("Content type is graphics.");
            return MediaType.GRAPHICS;
        }

        LOGGER.debug("Content type is text.");
        return MediaType.TEXT;
    }

    private Map<String, LocalizedContent> getContentMap(List<LocalizedContent> content) {
        final Map<String, LocalizedContent> contentMap = new HashMap<>();

        for (final LocalizedContent localizedContent : content) {
            contentMap.put(localizedContent.getLanguageCode(), localizedContent);
        }
        return contentMap;
    }
}
