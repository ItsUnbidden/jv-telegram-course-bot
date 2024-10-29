package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import org.springframework.lang.NonNull;

public interface ContentManager {
    @NonNull
    LocalizedContentHandler<? extends Content> getHandler(
            @NonNull MediaType contentType);

    @NonNull
    LocalizedContent getById(@NonNull Long id);
}
