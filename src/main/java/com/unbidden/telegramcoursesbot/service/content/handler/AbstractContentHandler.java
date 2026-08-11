package com.unbidden.telegramcoursesbot.service.content.handler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.exception.SendContentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;

public abstract class AbstractContentHandler<T extends LocalizedContent> implements LocalizedContentHandler<T> {
    protected final LocalizationLoader localizationLoader;
    
    public AbstractContentHandler(LocalizationLoader localizationLoader) {
        this.localizationLoader = localizationLoader;
    }

    @Override
    public List<Message> sendContent(UserEntity user, Bot bot, LocalizedContent content) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");

        try {
            return sendContentAsync(user, bot, content).join();
        } catch (CompletionException e) {
            throw new SendContentException("Failed to send content " + content.getId() + ".",
                    localizationLoader.localize(Localizations.Error.SEND_CONTENT, user), e.getCause());
        }
    }

    @Override
    public CompletableFuture<List<Message>> sendContentAsync(UserEntity user, Bot bot, LocalizedContent content) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");

        return sendContentInBulkAsync(List.of(user.getId()), bot, content).getFirst();
    }
}
