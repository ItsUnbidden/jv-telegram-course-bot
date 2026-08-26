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
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.LocalizedException;
import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
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

    public LocalizedContent parseAndPersistContent(UserEntity user, Bot bot, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        return contentService.parseAndPersistContent(user, bot, messages);
    }

    public LocalizedContent parseAndPersistContent(UserEntity user, Bot bot,
            List<Message> messages, String languageCode) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        return contentService.parseAndPersistContent(user, bot, messages, languageCode);
    }

    public LocalizedContent parseAndPersistContent(UserEntity user, Bot bot,
            List<Message> messages, List<MediaType> allowedContentTypes) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");
        Assert.notNull(allowedContentTypes, "allowedContentTypes cannot be null");

        return contentService.parseAndPersistContent(user, bot, messages, allowedContentTypes);
    }

    public LocalizedContent parseAndPersistContent(UserEntity user, Bot bot,
            List<Message> messages, String languageCode, List<MediaType> allowedContentTypes) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notNull(allowedContentTypes, "allowedContentTypes cannot be null");

        return contentService.parseAndPersistContent(user, bot, messages, languageCode, allowedContentTypes);
    }

    public void addNewLocalization(UserEntity user, Bot bot, Long mappingId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        final String languageCode;
        if (messages.size() > 1 && validatorUtil.checkLanguageCode(user, messages.getLast())) {
            languageCode = messages.getLast().getText().trim();
            messages.removeLast();
        } else {
            languageCode = user.getLanguageCode();
        }

        final ContentMapping mapping = contentService.addNewLocalization(user, bot, mappingId, languageCode, messages);
        final LocalizedContent content = mapping.getContent().getLast();

        LOGGER.info("New content " + content.getId() + " has been added to mapping " + mapping.getId() + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.ADD_NEW_LOCALIZATION_SUCCESS, user,
                new Localizations.Service.AddNewLocalizationSuccessParams(mappingId, content.getId())));
        LOGGER.debug("Message sent.");
    }

    public void removeLocalization(UserEntity user, Bot bot, Long mappingId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);
        if (!validatorUtil.checkLanguageCode(user, messages.getFirst())) {
            throw new InvalidDataSentException("Language code is required to remove a "
                    + "localization from a mapping.", loader.localize(Localizations.Error.LANGUAGE_CODE_REQUIRED, user));
        }
        final String languageCode = messages.getFirst().getText().trim();

        if (contentService.removeLocalization(user, bot, mappingId, languageCode)) {
            LOGGER.info("Localization with code " + languageCode + " has been removed from mapping " + mappingId + ".");

            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, loader.localize(
                    Localizations.Service.REMOVE_LOCALIZATION_FROM_MAPPING_SUCCESS, user,
                        new Localizations.Service.RemoveLocalizationFromMappingSuccessParams(mappingId, languageCode)));
            LOGGER.debug("Message sent.");
            return;
        }
        final ContentMapping mapping = entityUtil.getMappingById(user, bot, mappingId);

        throw new InvalidDataSentException("No elements were deleted since there is no "
                + "localization with language code " + languageCode, loader
                .localize(Localizations.Error.NO_LOCALIZATIONS_DELETED, user,
                    new Localizations.Error.NoLocalizationsDeletedParams(languageCode, mapping.getContent().stream()
                        .map(c -> c.getLanguageCode())
                        .collect(Collectors.joining(", ")))));
    }

    public List<Message> sendContent(UserEntity user, Bot bot, Long contentId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        final LocalizedContent content = entityUtil.getLocalizedContentById(user, bot, contentId);
        
        return sendContent(user, bot, content);
    }

    /**
     * Sends the content. It must be initialized, but the depth is irrelevant.
     * @param user to whom the content will be sent
     * @param bot
     * @param content
     * @return list of the sent messages
     */
    public List<Message> sendContent(UserEntity user, Bot bot, LocalizedContent content) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");
        
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentHandlerManager.getHandler(content.getType());

            return handler.sendContent(user, bot, content);
        } catch (NoImplementationException e) {
            throw new LocalizedException("Unknown media type", loader.localize(Localizations.Error.UNKNOWN_MEDIA_TYPE, user));
        }
    }

    /**
     * Asynchronosly sends a specific content.
     * @param user
     * @param bot
     * @param content
     * @return {@link CompletableFuture} of the request that was sent.
     */
    public CompletableFuture<List<Message>> sendContentAsync(UserEntity user, Bot bot, Long contentId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        final LocalizedContent content = entityUtil.getLocalizedContentById(user, bot, contentId);
        
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentHandlerManager.getHandler(content.getType());

            return handler.sendContentAsync(user, bot, content);
        } catch (NoImplementationException e) {
            throw new LocalizedException("Unknown media type", loader.localize(Localizations.Error.UNKNOWN_MEDIA_TYPE, user));
        }
    }

    /**
     * Asynchronosly sends the content to a list of users. It must be initialized, but the depth is irrelevant.
     * @param sender of the content
     * @param bot
     * @param targets of the operation
     * @param content
     * @return a list of {@link CompletableFuture} where each future represents one request that was sent.
     */
    public List<CompletableFuture<List<Message>>> sendContentInBulkAsync(UserEntity sender, Bot bot,
            List<Long> targetIds, LocalizedContent content) {
        Assert.notNull(sender, "sender cannot be null");
        Assert.notNull(targetIds, "targetIds cannot be null");
        Assert.notEmpty(targetIds, "targetIds cannot be empty");
        Assert.noNullElements(targetIds, "targetIds cannot contain null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");
        
        try {
            final LocalizedContentHandler<? extends LocalizedContent> handler = contentHandlerManager.getHandler(content.getType());

            return handler.sendContentInBulkAsync(targetIds, bot, content);
        } catch (NoImplementationException e) {
            throw new LocalizedException("Unknown media type", loader.localize(Localizations.Error.UNKNOWN_MEDIA_TYPE, sender));
        }
    }

    public List<Message> sendLocalizedContent(UserEntity user, Bot bot, Long mappingId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");

        return sendContent(user, bot, getLocalizedContentFromMapping(user, bot,
                entityUtil.getMappingById(user, bot, mappingId)));
    }

    public String getLocalizedText(UserEntity user, Bot bot, Long mappingId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");

        final LocalizedContent content = getLocalizedContentFromMapping(user, bot,
                entityUtil.getMappingById(user, bot, mappingId));

        return content.getData();
    }

    /**
     * Mapping <b>must have</b> all of its text contents initialized. Entities are currently not supported.
     * @param user
     * @param bot
     * @param mapping
     * @return the text content
     */
    public String getLocalizedText(UserEntity user, Bot bot, ContentMapping mapping) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(mapping, "mapping cannot be null");

        final LocalizedContent content = getLocalizedContentFromMapping(user, bot, mapping);

        return content.getData();
    }

    
    private LocalizedContent getLocalizedContentFromMapping(UserEntity user, Bot bot, ContentMapping mapping) {
        final Map<String, LocalizedContent> contentMap = getContentMap(mapping.getContent());

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
