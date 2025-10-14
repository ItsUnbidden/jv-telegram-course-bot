package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.content.Content;
import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface LocalizedContentHandler<T extends Content> extends ContentHandler<T> {
    T parseLocalized(@NonNull List<Message> messages, @NonNull Bot bot, boolean isLocalized);

    @Override
    @NonNull
    default T parse(@NonNull List<Message> messages, @NonNull Bot bot) {
        return parseLocalized(messages, bot, false);
    }

    T parseLocalized(@NonNull List<Message> messages, @NonNull Bot bot,
            @NonNull String localizationName, @NonNull String languageCode);
}
