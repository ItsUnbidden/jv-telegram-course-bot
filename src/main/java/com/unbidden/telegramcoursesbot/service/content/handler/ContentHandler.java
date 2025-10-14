package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface ContentHandler<T extends Content> {
    @NonNull
    T parse(@NonNull List<Message> messages, @NonNull Bot bot);

    @NonNull
    List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            @NonNull Bot bot);

    @NonNull
    List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            @NonNull Bot bot, boolean isProtected, boolean skipText);

    @NonNull
    Optional<T> findById(@NonNull Long id);

    @NonNull
    T persist(@NonNull Content content);

    @NonNull
    MediaType getContentType();
}
