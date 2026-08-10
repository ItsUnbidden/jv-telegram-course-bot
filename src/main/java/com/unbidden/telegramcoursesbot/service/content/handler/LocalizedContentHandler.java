package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface LocalizedContentHandler<T extends LocalizedContent> {
    default T parseAndPersist(Bot bot, List<Message> messages, String languageCode) {
        return parseAndPersist(bot, messages, languageCode, false);
    }

    T parseAndPersist(Bot bot, List<Message> messages, String languageCode, boolean isProtected);

    List<Message> sendContent(UserEntity user, Bot bot, LocalizedContent content);

    CompletableFuture<List<Message>> sendContentAsync(UserEntity user, Bot bot, LocalizedContent content);

    List<CompletableFuture<List<Message>>> sendContentInBulkAsync(List<Long> userIds, Bot bot, LocalizedContent content);

    MediaType getContentType();
}
