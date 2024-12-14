package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
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
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {
    private static final String ERROR_UNKNOWN_MEDIA_TYPE = "error_unknown_media_type";

    private static final Logger LOGGER = LogManager.getLogger(ContentServiceImpl.class);
    
    private static final String PARAM_SENT_CONTENT_MEDIA_TYPE = "${sentContentMediaType}";
    private static final String PARAM_ALLOWED_MEDIA_TYPES = "${allowedMediaTypes}";

    private static final String ERROR_CONTENT_MEDIA_GROUP_DOES_NOT_MATCH =
            "error_content_media_group_does_not_match";
    private static final String ERROR_CONTENT_TEXT_AND_CAPTIONS = "error_content_text_and_captions";
    private static final String ERROR_CONTENT_SEVERAL_TEXT = "error_content_several_text";
    private static final String ERROR_CONTENT_AUDIO_GROUP_FAILURE =
            "error_content_audio_group_failure";
    private static final String ERROR_CONTENT_DOCUMENT_GROUP_FAILURE =
            "error_content_document_group_failure";
    private static final String ERROR_CONTENT_GRAPHICS_GROUP_FAILURE =
            "error_content_graphics_group_failure";
    private static final String ERROR_CONTENT_MAPPING_NOT_FOUND =
            "error_content_mapping_not_found";

    private static final String MEDIA_TYPES_DIVIDER = " ";

    private final ContentMappingRepository contentMappingRepository;

    private final ContentManager contentManager;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TextUtil textUtil;

    @Value("${telegram.bot.message.language.priority}")
    private String languagePriorityStr;

    @Override
    @NonNull
    public LocalizedContent parseAndPersistContent(@NonNull Bot bot,
            @NonNull List<Message> messages) {
        return parseAndPersistContent(bot, messages, List.of());
    }

    @Override
    @NonNull
    public LocalizedContent parseAndPersistContent(@NonNull Bot bot,
            @NonNull List<Message> messages, boolean isLocalized) {
        return persistContent(messages, List.of(), bot, isLocalized);
    }

    @Override
    @NonNull
    public LocalizedContent parseAndPersistContent(@NonNull Bot bot,
            @NonNull List<Message> messages, @NonNull List<MediaType> allowedContentTypes) {
        return persistContent(messages, allowedContentTypes, bot, false);
    }

    @Override
    @NonNull
    public LocalizedContent parseAndPersistContent(@NonNull Bot bot,
            @NonNull List<Message> messages, @NonNull String localizationName,
            @NonNull String languageCode) {
        LOGGER.info("Initiating parsing of a message to content with predefined localization...");
        final MediaType messagesContentType = defineContentType(messages);

        try {
            final LocalizedContentHandler<? extends Content> handler = contentManager
                    .getHandler(messagesContentType);
            final Content content = handler.parseLocalized(messages, bot, localizationName,
                    languageCode);

            content.setType(messagesContentType);

            return (LocalizedContent)handler.persist(content);
        } catch (NoImplementationException e) {
            throw new InvalidDataSentException("Unknown media type", localizationLoader
                    .getLocalizationForUser(ERROR_UNKNOWN_MEDIA_TYPE, userService.getUser(
                    messages.get(0).getFrom().getId(), userService.getDiretor())));
        }
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user, @NonNull Bot bot) {
        try {
            return contentManager.getHandler(content.getType()).sendContent(content, user, bot);
        } catch (NoImplementationException e) {
            throw new InvalidDataSentException("Unknown media type", localizationLoader
                    .getLocalizationForUser(ERROR_UNKNOWN_MEDIA_TYPE, user));
        }
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            @NonNull Bot bot, boolean isProtected, boolean skipText) {
        try {
            return contentManager.getHandler(content.getType())
                    .sendContent(content, user, bot, isProtected, skipText);
        } catch (NoImplementationException e) {
            throw new InvalidDataSentException("Unknown media type", localizationLoader
                    .getLocalizationForUser(ERROR_UNKNOWN_MEDIA_TYPE, user));
        }
    }
    
    @Override
    @NonNull
    public List<Message> sendLocalizedContent(@NonNull ContentMapping contentMapping,
            @NonNull UserEntity user, @NonNull Bot bot) {
        Assert.notEmpty(contentMapping.getContent(), "Content mapping cannot be empty");
        final Map<String, LocalizedContent> contentMap =
                getContentMap(contentMapping.getContent());

        if (contentMap.containsKey(user.getLanguageCode())) {
            LOGGER.debug("Localized content in group " + contentMapping.getId()
                    + " for user " + user.getId() + "'s prefered code " + user.getLanguageCode()
                    + " is available.");
            return sendContent(getById(contentMap.get(user.getLanguageCode()).getId(), user, bot),
                    user, bot, true, !contentMapping.isTextEnabled());
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
                    return sendContent(getById(contentMap.get(code).getId(), user, bot),
                            user, bot, true, !contentMapping.isTextEnabled());
                }
            }
        }
        final LocalizedContent firstAvailableContent = contentMapping.getContent().get(0);
        LOGGER.warn("There is no available content in group " + contentMapping.getId()
                + " for any of the priority language codes. First content in the list (Id: "
                + firstAvailableContent.getId() + ") will be used instead.");
        return sendContent(getById(firstAvailableContent.getId(), user, bot), user, bot,
                true, !contentMapping.isTextEnabled());
    }

    @Override
    @NonNull
    public LocalizedContent getById(@NonNull Long id, @NonNull UserEntity user,
            @NonNull Bot bot) {
        return contentManager.getById(id, user, bot);
    }

    @Override
    @NonNull
    public ContentMapping getMappingById(@NonNull Long id, @NonNull UserEntity user) {
        return contentMappingRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Content mapping with id " + id
                + " does not exist", localizationLoader.getLocalizationForUser(
                ERROR_CONTENT_MAPPING_NOT_FOUND, user)));
    }

    @Override
    @NonNull
    public ContentMapping saveMapping(@NonNull ContentMapping mapping) {
        return contentMappingRepository.save(mapping);
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

    @Override
    @NonNull
    public ContentMapping addNewLocalization(@NonNull ContentMapping mapping,
            @NonNull LocalizedContent content, @NonNull Bot bot) {
        mapping.getContent().add(content);
        return contentMappingRepository.save(mapping);
    }

    @Override
    public boolean removeLocalization(@NonNull ContentMapping mapping,
            @NonNull String languageCode) {
        if (mapping.getContent().removeIf(c -> c.getLanguageCode().equals(languageCode))) {
            contentMappingRepository.save(mapping);
            return true;
        }
        return false;
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
        final UserEntity user = userService.getUser(messages.get(0).getFrom().getId(),
                userService.getDiretor());

        if (numberOfText != 0 && isCaptionPresent) {
            throw new InvalidDataSentException("Captions and text in the "
                    + "same content are not supported", localizationLoader.getLocalizationForUser(
                    ERROR_CONTENT_TEXT_AND_CAPTIONS, user));
        }
        if (numberOfText > 1) {
            throw new InvalidDataSentException("Several text messages are not supported",
                    localizationLoader.getLocalizationForUser(ERROR_CONTENT_SEVERAL_TEXT, user));
        }

        if (numberOfAudio != 0) {
            if (messages.size() != numberOfAudio + numberOfText) {
                throw new InvalidDataSentException("Audio files can only be "
                        + "grouped with other audio files and text", localizationLoader
                        .getLocalizationForUser(ERROR_CONTENT_AUDIO_GROUP_FAILURE, user));
            }
            LOGGER.info("Content type is audio.");
            return MediaType.AUDIO;
        }
        
        if (numberOfDocuments != 0) {
            if (messages.size() != numberOfDocuments + numberOfText) {
                throw new InvalidDataSentException("Documents can only be "
                        + "grouped with other documents and text", localizationLoader
                        .getLocalizationForUser(ERROR_CONTENT_DOCUMENT_GROUP_FAILURE, user));
            }
            LOGGER.info("Content type is document.");
            return MediaType.DOCUMENT;
        }

        if (numberOfGraphics != 0) {
            if (messages.size() != numberOfGraphics + numberOfText) {
                throw new InvalidDataSentException("Videos and photos cannot be grouped "
                        + "with other media types", localizationLoader
                        .getLocalizationForUser(ERROR_CONTENT_GRAPHICS_GROUP_FAILURE, user));
            }
            LOGGER.info("Content type is graphic.");
            return MediaType.GRAPHICS;
        }

        LOGGER.info("Content type is text.");
        return MediaType.TEXT;
    }

    private LocalizedContent persistContent(List<Message> messages,
            List<MediaType> allowedContentTypes, Bot bot,
            boolean isLocalized) {
        LOGGER.info("Initiating parsing of a message to content.");
        final MediaType messagesContentType = defineContentType(messages);
        final UserEntity user = userService.getUser(messages.get(0).getFrom().getId(),
                userService.getDiretor());

        if (!allowedContentTypes.isEmpty()) {
            LOGGER.info("Allowed content types are " + allowedContentTypes + ".");

            if (!allowedContentTypes.contains(messagesContentType)) {
                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_ALLOWED_MEDIA_TYPES, allowedContentTypes);
                parameterMap.put(PARAM_SENT_CONTENT_MEDIA_TYPE, messagesContentType);
                throw new InvalidDataSentException("Allowed content types are "
                        + allowedContentTypes + " but user sent messages of type "
                        + messagesContentType, localizationLoader
                        .getLocalizationForUser(ERROR_CONTENT_MEDIA_GROUP_DOES_NOT_MATCH, user,
                        parameterMap));
            }
        }
        
        try {
            final LocalizedContentHandler<? extends Content> handler = contentManager
                    .getHandler(messagesContentType);
            final Content content = (isLocalized) ? handler.parseLocalized(messages,
                    bot, isLocalized) : handler.parse(messages, bot);
            content.setType(messagesContentType);
        return (LocalizedContent)handler.persist(content);
        } catch (NoImplementationException e) {
            throw new InvalidDataSentException("Unknown media type", localizationLoader
                    .getLocalizationForUser(ERROR_UNKNOWN_MEDIA_TYPE, user));
        }
    }

    private Map<String, LocalizedContent> getContentMap(List<LocalizedContent> content) {
        final Map<String, LocalizedContent> contentMap = new HashMap<>();

        for (LocalizedContent localizedContent : content) {
            contentMap.put(localizedContent.getLanguageCode(), localizedContent);
        }
        return contentMap;
    }
}
