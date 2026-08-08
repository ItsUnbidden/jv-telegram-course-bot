package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.LocalizedContentRepository;
import com.unbidden.telegramcoursesbot.repository.MarkerAreaRepository;

import jakarta.transaction.Transactional;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class TextContentHandler implements LocalizedContentHandler<LocalizedContent> {
    private final LocalizedContentRepository contentRepository;

    private final MarkerAreaRepository markerAreaRepository;

    private final ClientManager clientManager;

    @NonNull
    @Override
    @Transactional
    public LocalizedContent parseAndPersist(@NonNull Bot bot, @NonNull List<Message> messages,
            @NonNull String languageCode, boolean isProtected) {
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
    @NonNull
    public List<Message> sendContent(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull LocalizedContent content) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");

        return List.of(clientManager.getClient(bot).sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(content.getData())
                .entities(markerAreaRepository.findByContentId(content.getId()).stream()
                        .map(MarkerArea::toMessageEntity).toList())
                .protectContent(content.isProtected())
                .build()));
    }

    @Override
    @NonNull
    public MediaType getContentType() {
        return MediaType.TEXT;
    }
}
