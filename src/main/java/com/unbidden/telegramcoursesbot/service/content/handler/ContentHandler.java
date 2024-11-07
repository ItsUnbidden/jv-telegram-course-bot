package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface ContentHandler<T extends Content> {
    @NonNull
    T parse(@NonNull List<Message> messages);

    @NonNull
    List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user);

    @NonNull
    List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            boolean isProtected, boolean skipText);

    @NonNull
    Optional<T> findById(@NonNull Long id);

    @NonNull
    T persist(@NonNull Content content);

    @NonNull
    MediaType getContentType();
}
