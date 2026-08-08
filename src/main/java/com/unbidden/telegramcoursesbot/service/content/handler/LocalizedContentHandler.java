package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;

import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface LocalizedContentHandler<T extends LocalizedContent> {
    default T parseAndPersist(@NonNull Bot bot, @NonNull List<Message> messages, @NonNull String languageCode) {
        return parseAndPersist(bot, messages, languageCode, false);
    }

    T parseAndPersist(@NonNull Bot bot, @NonNull List<Message> messages, @NonNull String languageCode, boolean isProtected);

    @NonNull
    List<Message> sendContent(@NonNull UserEntity user, @NonNull Bot bot, @NonNull LocalizedContent content);

    @NonNull
    MediaType getContentType();
}
