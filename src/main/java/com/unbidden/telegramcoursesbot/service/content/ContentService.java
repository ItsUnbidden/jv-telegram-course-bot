package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;


public interface ContentService {
    @NonNull
    LocalizedContent parseAndPersistContent(@NonNull List<Message> messages);

    @NonNull
    LocalizedContent parseAndPersistContent(@NonNull List<Message> messages, boolean isLocalized);

    @NonNull
    LocalizedContent parseAndPersistContent(@NonNull List<Message> messages,
            @NonNull List<MediaType> allowedContentTypes);

    @NonNull
    LocalizedContent parseAndPersistContent(@NonNull List<Message> messages,
            @NonNull String localizationName, @NonNull String languageCode);

    @NonNull
    @Deprecated
    LocalizedContent parseAndUpdateContent(@NonNull Long contentId,
            @NonNull List<Message> messages);

    @NonNull
    @Deprecated
    LocalizedContent parseAndUpdateContent(@NonNull Long contentId,
            @NonNull List<Message> messages, boolean isLocalized);

    @NonNull
    @Deprecated
    LocalizedContent parseAndUpdateContent(@NonNull Long contentId,
            @NonNull List<Message> messages, @NonNull List<MediaType> allowedContentTypes);

    @NonNull
    List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user);

    @NonNull
    List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            boolean isProtected, boolean skipText);

    @NonNull
    List<Message> sendLocalizedContent(@NonNull ContentMapping contentMapping,
            @NonNull UserEntity user);

    @NonNull
    LocalizedContent getById(@NonNull Long id, @NonNull UserEntity user);

    @NonNull
    ContentMapping getMappingById(@NonNull Long id, @NonNull UserEntity user);

    @NonNull
    ContentMapping saveMapping(@NonNull ContentMapping mapping);

    @NonNull
    List<MediaType> parseMediaTypes(@NonNull String mediaTypesStr);

    @NonNull
    ContentMapping addNewLocalization(@NonNull ContentMapping mapping,
            @NonNull LocalizedContent content);

    boolean removeLocalization(@NonNull ContentMapping mapping, @NonNull String languageCode);
}
