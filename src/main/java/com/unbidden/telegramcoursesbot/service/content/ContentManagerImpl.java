package com.unbidden.telegramcoursesbot.service.content;

import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.service.content.handler.LocalizedContentHandler;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import jakarta.annotation.PostConstruct;
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

    private static final String ERROR_BOT_VISIBILITY_MISMATCH = "error_bot_visibility_mismatch";
    private static final String ERROR_CONTENT_NOT_FOUND = "error_content_not_found";
    
    private final LocalizationLoader localizationLoader;

    @PostConstruct
    public void init() {
        for (LocalizedContentHandler<? extends Content> handler : handlers) {
            handlerMap.put(handler.getContentType(), handler);
        }
    }

    @Override
    @NonNull
    public LocalizedContentHandler<? extends Content> getHandler(
            @NonNull MediaType contentType) throws NoImplementationException {
        final LocalizedContentHandler<? extends Content> potentialHandler =
                handlerMap.get(contentType);
        if (potentialHandler != null) {
            return potentialHandler;
        }
        throw new NoImplementationException("There is no handler implementation for content type "
                + contentType);
    }

    @Override
    @NonNull
    public LocalizedContent getById(@NonNull Long id, @NonNull UserEntity user,
            @NonNull Bot bot) {
        for (LocalizedContentHandler<? extends Content> handler : handlers) {
            final Optional<? extends Content> potentialContent = handler.findById(id);

            if (potentialContent.isPresent()) {
                if (!potentialContent.get().getBot().equals(bot)) {
                    throw new AccessDeniedException("Content with id " + id
                            + " is not available for bot " + bot.getName(), localizationLoader
                            .getLocalizationForUser(ERROR_BOT_VISIBILITY_MISMATCH, user));
                }
                return (LocalizedContent)potentialContent.get();
            }
        }
        throw new EntityNotFoundException("Content with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_CONTENT_NOT_FOUND, user));
    }
}
