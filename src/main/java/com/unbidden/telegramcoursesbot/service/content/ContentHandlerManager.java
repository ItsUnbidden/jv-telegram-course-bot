package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentHandlerManager {
    private static final Map<MediaType, LocalizedContentHandler<? extends Content>> handlerMap =
            new HashMap<>();

    private final List<LocalizedContentHandler<? extends Content>> handlers;
    
    @PostConstruct
    public void init() {
        for (final LocalizedContentHandler<? extends Content> handler : handlers) {
            handlerMap.put(handler.getContentType(), handler);
        }
    }

    @NonNull
    public LocalizedContentHandler<? extends LocalizedContent> getHandler(
            @NonNull MediaType contentType) throws NoImplementationException {
        final LocalizedContentHandler<? extends Content> potentialHandler =
                handlerMap.get(contentType);
        if (potentialHandler != null) {
            return potentialHandler;
        }
        throw new NoImplementationException("There is no handler implementation for content type "
                + contentType);
    }
}
