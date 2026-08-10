package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.LocalizedContentRepository;
import com.unbidden.telegramcoursesbot.repository.MarkerAreaRepository;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public class TextContentHandler extends AbstractContentHandler<LocalizedContent> {
    private final LocalizedContentRepository contentRepository;
    
    private final MarkerAreaRepository markerAreaRepository;

    private final ClientManager clientManager;

    public TextContentHandler(LocalizedContentRepository contentRepository,
            MarkerAreaRepository markerAreaRepository,
            ClientManager clientManager,
            LocalizationLoader localizationLoader) {
        super(localizationLoader);
        this.contentRepository = contentRepository;
        this.markerAreaRepository = markerAreaRepository;
        this.clientManager = clientManager;
    }

    @Override
    @Transactional
    public LocalizedContent parseAndPersist(Bot bot, List<Message> messages, String languageCode, boolean isProtected) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");

        final LocalizedContent content = new LocalizedContent();
        final Message message = messages.getFirst();
        final List<MarkerArea> markers = (message.getEntities() != null) ? message.getEntities()
                .stream().map(e -> new MarkerArea(e, content))
                .toList() : List.of();
        
        content.setBot(bot);
        content.setData(message.getText());
        content.setLanguageCode(languageCode);
        content.setType(getContentType());
        content.setProtected(isProtected);
        contentRepository.save(content);
        markerAreaRepository.saveAll(markers);
        return content;
    }

    @Override
    public List<CompletableFuture<List<Message>>> sendContentInBulkAsync(List<Long> userIds, Bot bot, LocalizedContent content) {
        Assert.notNull(userIds, "userIds cannot be null");
        Assert.notEmpty(userIds, "userIds cannot be empty");
        Assert.noNullElements(userIds, "userIds cannot contain null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");

        final List<MessageEntity> entities = markerAreaRepository.findByContentId(content.getId()).stream()
                .map(MarkerArea::toMessageEntity).toList();
        final var client = clientManager.getClient(bot);

        return userIds.stream().map(id -> client.sendMessageAsync(SendMessage.builder()
                .chatId(id)
                .text(content.getData())
                .entities(entities)
                .protectContent(content.isProtected())
                .build()).thenApply(m -> List.of(m))).toList();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.TEXT;
    }
}
