package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentManagerImpl implements ContentManager {
    private static final Map<MediaType, LocalizedContentHandler<? extends Content>> handlerMap =
            new HashMap<>();

    private final List<LocalizedContentHandler<? extends Content>> handlers;

    public void init() {
        for (LocalizedContentHandler<? extends Content> handler : handlers) {
            handlerMap.put(handler.getContentType(), handler);
        }
    }

    @Override
    @NonNull
    public LocalizedContentHandler<? extends Content> getHandler(
            @NonNull MediaType contentType) {
        final LocalizedContentHandler<? extends Content> potentialHandler =
                handlerMap.get(contentType);
        if (potentialHandler != null) {
            return potentialHandler;
        }
        throw new NoImplementationException("There is no parser implementation for content type "
                + contentType);
    }

    @Override
    @NonNull
    public Content getById(@NonNull Long id) {
        for (LocalizedContentHandler<? extends Content> handler : handlers) {
            final Optional<? extends Content> potentialContent = handler.findById(id);

            if (potentialContent.isPresent()) {
                return potentialContent.get();
            }
        }
        throw new EntityNotFoundException("Content with id " + id + " does not exist");
    }
}
