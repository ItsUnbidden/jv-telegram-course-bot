package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface LocalizedContentHandler<T extends LocalizedContent> {
    default T parseAndPersist(BotRole botRole, List<Message> messages, String languageCode) {
        return parseAndPersist(botRole, messages, languageCode, false);
    }

    T parseAndPersist(BotRole botRole, List<Message> messages, String languageCode, boolean isProtected);

    List<SendMessageResultDto> sendContent(BotRole botRole, LocalizedContent content);

    CompletableFuture<List<SendMessageResultDto>> sendContentAsync(BotRole botRole, LocalizedContent content);

    List<CompletableFuture<List<SendMessageResultDto>>> sendContentInBulkAsync(List<BotRole> targetRoles, LocalizedContent content);

    MediaType getContentType();
}
