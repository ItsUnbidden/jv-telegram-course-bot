package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
    public LocalizedContent parseAndPersist(BotRole botRole, List<Message> messages, String languageCode, boolean isProtected) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");
        Assert.notNull(languageCode, "languageCode cannot be null");

        final LocalizedContent content = new LocalizedContent();
        final Message message = messages.getFirst();
        final List<MarkerArea> markers = (message.getEntities() != null) ? message.getEntities()
                .stream().map(e -> new MarkerArea(e, content))
                .toList() : List.of();
        
        content.setBot(botRole.getBot());
        content.setData(message.getText());
        content.setLanguageCode(languageCode);
        content.setType(getContentType());
        content.setProtected(isProtected);
        contentRepository.save(content);
        markerAreaRepository.saveAll(markers);
        return content;
    }

    @Override
    public List<CompletableFuture<List<SendMessageResultDto>>> sendContentInBulkAsync(List<BotRole> targetRoles, LocalizedContent content) {
        Assert.notEmpty(targetRoles, "targetRoles cannot be empty or null");
        Assert.noNullElements(targetRoles, "targetRoles cannot contain null");
        Assert.notNull(content, "content cannot be null");

        final List<MessageEntity> entities = markerAreaRepository.findByContentId(content.getId()).stream()
                .map(MarkerArea::toMessageEntity).toList();

        return targetRoles.stream().map(br -> {
            try {
                return clientManager.getClient(br.getBot()).executeAsync(SendMessage.builder()
                    .chatId(br.getUser().getId())
                    .text(content.getData())
                    .entities(entities)
                    .protectContent(content.isProtected())
                    .build()).handle((m, t) -> {
                        if (t != null) {
                            return List.of(new SendMessageResultDto(new TelegramException("Failed to send a text content.",
                                    localizationLoader.localize(Localizations.Error.SEND_CONTENT, br), t)));
                        } else {
                            return List.of(new SendMessageResultDto(m));
                        }
                    });
            } catch (TelegramApiException e) {
                return CompletableFuture.completedFuture(List.of(new SendMessageResultDto(new TelegramException("Failed to send a text content.",
                        localizationLoader.localize(Localizations.Error.SEND_CONTENT, br), e))));
            }
        }).toList();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.TEXT;
    }
}
