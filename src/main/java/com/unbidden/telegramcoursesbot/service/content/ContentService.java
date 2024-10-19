package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface ContentService {
    @NonNull
    Content parseAndPersistContent(@NonNull List<Message> messages);

    @NonNull
    Content parseAndPersistContent(@NonNull List<Message> messages, boolean isLocalized);

    @NonNull
    Content parseAndPersistContent(@NonNull List<Message> messages,
            @NonNull List<MediaType> allowedContentTypes);

    @NonNull
    Content parseAndUpdateContent(@NonNull Long contentId, @NonNull List<Message> messages);

    @NonNull
    Content parseAndUpdateContent(@NonNull Long contentId, @NonNull List<Message> messages,
            boolean isLocalized);

    @NonNull
    Content parseAndUpdateContent(@NonNull Long contentId, @NonNull List<Message> messages,
            @NonNull List<MediaType> allowedContentTypes);

    @NonNull
    List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user);

    @NonNull
    Content getById(@NonNull Long id);

    @NonNull
    List<MediaType> parseMediaTypes(@NonNull String mediaTypesStr);
}
