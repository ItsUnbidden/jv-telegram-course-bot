package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.model.content.Content;
import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface LocalizedContentHandler<T extends Content> extends ContentHandler<T> {
    T parseLocalized(@NonNull List<Message> messages, boolean isLocalized);

    @Override
    @NonNull
    default T parse(@NonNull List<Message> messages) {
        return parseLocalized(messages, false);
    }

    T parseLocalized(@NonNull List<Message> messages, @NonNull String localizationName,
            @NonNull String languageCode);
}
