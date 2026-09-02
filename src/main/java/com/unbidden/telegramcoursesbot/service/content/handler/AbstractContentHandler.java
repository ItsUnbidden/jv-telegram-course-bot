package com.unbidden.telegramcoursesbot.service.content.handler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.util.Assert;

import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;

public abstract class AbstractContentHandler<T extends LocalizedContent> implements LocalizedContentHandler<T> {
    protected final LocalizationLoader localizationLoader;
    
    public AbstractContentHandler(LocalizationLoader localizationLoader) {
        this.localizationLoader = localizationLoader;
    }

    @Override
    public List<SendMessageResultDto> sendContent(BotRole botRole, LocalizedContent content) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(content, "content cannot be null");

        try {
            return sendContentAsync(botRole, content).join();
        } catch (CompletionException e) {
            throw new RuntimeException("An exception occured while waiting for the send content future to complete. "
                    + "This is a bug, since all exceptions should be handled.", e.getCause());
        }
    }

    @Override
    public CompletableFuture<List<SendMessageResultDto>> sendContentAsync(BotRole botRole, LocalizedContent content) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(content, "content cannot be null");

        return sendContentInBulkAsync(List.of(botRole), content).getFirst();
    }
}
