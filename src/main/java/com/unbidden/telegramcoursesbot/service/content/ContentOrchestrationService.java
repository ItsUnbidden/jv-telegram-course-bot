package com.unbidden.telegramcoursesbot.service.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.LocalizedException;
import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(ContentOrchestrationService.class);

    private final ContentService contentService;

    private final ContentHandlerManager contentHandlerManager;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final TextUtil textUtil;

    private final ValidatorUtil validatorUtil;

    public LocalizedContent parseAndPersistContent(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        return contentService.parseAndPersistContent(botRole, messages);
    }

    public LocalizedContent parseAndPersistContent(BotRole botRole,
            List<Message> messages, String languageCode) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        return contentService.parseAndPersistContent(botRole, messages, languageCode);
    }

    public LocalizedContent parseAndPersistContent(BotRole botRole,
            List<Message> messages, List<MediaType> allowedContentTypes) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");
        Assert.notNull(allowedContentTypes, "allowedContentTypes cannot be null");

        return contentService.parseAndPersistContent(botRole, messages, allowedContentTypes);
    }

    public LocalizedContent parseAndPersistContent(BotRole botRole,
            List<Message> messages, String languageCode, List<MediaType> allowedContentTypes) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notNull(allowedContentTypes, "allowedContentTypes cannot be null");

        return contentService.parseAndPersistContent(botRole, messages, languageCode, allowedContentTypes);
    }

    public void addNewLocalization(BotRole botRole, Long mappingId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        final String languageCode;
        if (messages.size() > 1 && validatorUtil.checkLanguageCode(botRole, messages.getLast())) {
            languageCode = messages.getLast().getText().trim();
            messages.removeLast();
        } else {
            languageCode = botRole.getUser().getLanguageCode();
        }

        final ContentMapping mapping = contentService.addNewLocalization(botRole, mappingId, languageCode, messages);
        final LocalizedContent content = mapping.getContent().getLast();

        LOGGER.info("New content " + content.getId() + " has been added to mapping " + mapping.getId() + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.ADD_NEW_LOCALIZATION_SUCCESS, botRole,
                new Localizations.Service.AddNewLocalizationSuccessParams(mappingId, content.getId())));
        LOGGER.debug("Message sent.");
    }

    public void removeLocalization(BotRole botRole, Long mappingId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        if (!validatorUtil.checkLanguageCode(botRole, messages.getFirst())) {
            throw new InvalidDataSentException("Language code is required to remove a "
                    + "localization from a mapping.", loader.localize(Localizations.Error.LANGUAGE_CODE_REQUIRED, botRole));
        }
        final String languageCode = messages.getFirst().getText().trim();

        if (contentService.removeLocalization(botRole, mappingId, languageCode)) {
            LOGGER.info("Localization with code " + languageCode + " has been removed from mapping " + mappingId + ".");

            LOGGER.debug("Sending confirmation message...");
            clientManager.sendMessage(botRole, loader.localize(
                    Localizations.Service.REMOVE_LOCALIZATION_FROM_MAPPING_SUCCESS, botRole,
                        new Localizations.Service.RemoveLocalizationFromMappingSuccessParams(mappingId, languageCode)));
            LOGGER.debug("Message sent.");
            return;
        }
        final ContentMapping mapping = entityUtil.getMappingById(botRole, mappingId);

        throw new InvalidDataSentException("No elements were deleted since there is no "
                + "localization with language code " + languageCode, loader
                .localize(Localizations.Error.NO_LOCALIZATIONS_DELETED, botRole,
                    new Localizations.Error.NoLocalizationsDeletedParams(languageCode, mapping.getContent().stream()
                        .map(c -> c.getLanguageCode())
                        .collect(Collectors.joining(", ")))));
    }

    public List<SendMessageResultDto> sendContent(BotRole botRole, Long contentId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        final LocalizedContent content = entityUtil.getLocalizedContentById(botRole, contentId);
        
        return sendContent(botRole, content);
    }

    /**
     * Sends the content. It must be initialized, but the depth is irrelevant.
     * @param user to whom the content will be sent
     * @param bot
     * @param content
     * @return list of the sent messages
     */
    public List<SendMessageResultDto> sendContent(BotRole botRole, LocalizedContent content) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(content, "content cannot be null");
        
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentHandlerManager.getHandler(content.getType());

            return handler.sendContent(botRole, content);
        } catch (NoImplementationException e) {
            throw new LocalizedException("Unknown media type", loader.localize(Localizations.Error.UNKNOWN_MEDIA_TYPE, botRole));
        }
    }

    /**
     * Asynchronosly sends a specific content.
     * @param user
     * @param bot
     * @param content
     * @return {@link CompletableFuture} of the request that was sent.
     */
    public CompletableFuture<List<SendMessageResultDto>> sendContentAsync(BotRole botRole, Long contentId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        final LocalizedContent content = entityUtil.getLocalizedContentById(botRole, contentId);
        
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentHandlerManager.getHandler(content.getType());

            return handler.sendContentAsync(botRole, content);
        } catch (NoImplementationException e) {
            throw new LocalizedException("Unknown media type", loader.localize(Localizations.Error.UNKNOWN_MEDIA_TYPE, botRole));
        }
    }

    /**
     * Asynchronosly sends the content to a list of users. The content must be initialized, but the depth is irrelevant.
     * @param sender of the content
     * @param bot
     * @param targets of the operation
     * @param content
     * @return a list of {@link CompletableFuture} where each future represents one request that was sent.
     */
    public List<CompletableFuture<List<SendMessageResultDto>>> sendContentInBulkAsync(BotRole senderBotRole, List<BotRole> targetRoles, LocalizedContent content) {
        Assert.notNull(senderBotRole, "senderBotRole cannot be null");
        Assert.notEmpty(targetRoles, "targetRoles cannot be empty or null");
        Assert.noNullElements(targetRoles, "targetRoles cannot contain null");
        Assert.notNull(content, "content cannot be null");
        
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentHandlerManager.getHandler(content.getType());

            return handler.sendContentInBulkAsync(targetRoles, content);
        } catch (NoImplementationException e) {
            throw new LocalizedException("Unknown media type", loader.localize(Localizations.Error.UNKNOWN_MEDIA_TYPE, senderBotRole));
        }
    }

    public List<SendMessageResultDto> sendLocalizedContent(BotRole botRole, Long mappingId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");

        return sendContent(botRole, getLocalizedContentFromMapping(botRole,
                entityUtil.getMappingById(botRole, mappingId)));
    }

    public String getLocalizedText(BotRole botRole, Long mappingId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");

        final LocalizedContent content = getLocalizedContentFromMapping(botRole,
                entityUtil.getMappingById(botRole, mappingId));

        return content.getData();
    }

    /**
     * Mapping <b>must have</b> all of its text contents initialized. Entities are currently not supported.
     * @param user
     * @param bot
     * @param mapping
     * @return the text content
     */
    public String getLocalizedText(BotRole botRole, ContentMapping mapping) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(mapping, "mapping cannot be null");

        final LocalizedContent content = getLocalizedContentFromMapping(botRole, mapping);

        return content.getData();
    }

    
    private LocalizedContent getLocalizedContentFromMapping(BotRole botRole, ContentMapping mapping) {
        final Map<String, LocalizedContent> contentMap = getContentMap(mapping.getContent());
        final UserEntity user = botRole.getUser();

        if (contentMap.containsKey(user.getLanguageCode())) {
            LOGGER.debug("Localized content in mapping " + mapping.getId()
                    + " for user " + user.getId() + "'s prefered code " + user.getLanguageCode()
                    + " is available.");
            return contentMap.get(user.getLanguageCode());
        }
        LOGGER.debug("Localized content in mapping " + mapping.getId() + " for user "
                + user.getId() + "'s prefered code " + user.getLanguageCode()
                + " is not available. Looking over the language code priority list...");
        final List<String> languagePriority = textUtil.getLanguagePriority();

        for (final String code : languagePriority) {
            if (!code.equals(user.getLanguageCode())) {
                if (contentMap.containsKey(code)) {
                    LOGGER.debug("Localized content in mapping " + mapping.getId()
                            + " has been found for language code " + code + ".");
                    return contentMap.get(code);
                }
            }
        }
        final LocalizedContent firstAvailableContent = mapping.getContent().get(0);
        LOGGER.warn("There is no available content in mapping " + mapping.getId()
                + " for any of the priority language codes. First content in the list (Id: "
                + firstAvailableContent.getId() + ") will be used instead.");

        return mapping.getContent().get(0);
    }

    private Map<String, LocalizedContent> getContentMap(List<LocalizedContent> content) {
        final Map<String, LocalizedContent> contentMap = new HashMap<>();

        for (final LocalizedContent localizedContent : content) {
            contentMap.put(localizedContent.getLanguageCode(), localizedContent);
        }
        return contentMap;
    }
}
