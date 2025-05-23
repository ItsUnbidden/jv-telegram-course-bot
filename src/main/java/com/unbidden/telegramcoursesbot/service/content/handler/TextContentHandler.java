package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentTextData;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.LocalizedContentRepository;
import com.unbidden.telegramcoursesbot.repository.MarkerAreaRepository;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class TextContentHandler implements LocalizedContentHandler<LocalizedContent> {
    private static final Logger LOGGER = LogManager.getLogger(TextContentHandler.class);

    private final LocalizedContentRepository localizedContentRepository;

    private final MarkerAreaRepository markerAreaRepository;

    private final LocalizationLoader localizationLoader;

    private final UserService userService;

    private final CustomTelegramClient client;

    @Override
    public LocalizedContent parseLocalized(@NonNull List<Message> messages, boolean isLocalized) {
        final LocalizedContent localizedContent = new LocalizedContent();
        final Message message = messages.get(0);
        final List<MarkerArea> markers = (message.getEntities() != null) ? message.getEntities()
                .stream().map(e -> markerAreaRepository.save(new MarkerArea(e)))
                .toList() : List.of();

        localizedContent.setData(new ContentTextData(message.getText(),
                markers, isLocalized));
        localizedContent.setLanguageCode(userService.getUser(message
                .getFrom().getId()).getLanguageCode());
        return localizedContent;
    }

    @Override
    public LocalizedContent parseLocalized(@NonNull List<Message> messages,
            @NonNull String localizationName, @NonNull String languageCode) {
        final LocalizedContent localizedContent = new LocalizedContent();
        
        localizedContent.setData(new ContentTextData(localizationName, List.of(), true));
        localizedContent.setLanguageCode(languageCode);
        return localizedContent;
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user) {
        return sendContent(content, user, false, false);
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            boolean isProtected, boolean skipText) {
        if (skipText) {
            LOGGER.warn("Content " + content.getId() + " is of type " + content.getType()
                    + " but parameter to skip text is enabled, meaning no content will be sent.");
            return List.of();
        }
        final LocalizedContent localizedContent = (LocalizedContent)content;
        final Localization localization = (localizedContent.getData().isLocalization())
                ? localizationLoader.getLocalizationForUser(localizedContent
                    .getData().getData(), user)
                : new Localization(localizedContent.getData().getData());
        
        localization.setEntities(localizedContent.getData().getEntities().stream()
                .map(m -> m.toMessageEntity()).toList());
        return List.of(client.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .protectContent(isProtected)
                .build()));
    }

    @Override
    @NonNull
    public Optional<LocalizedContent> findById(@NonNull Long id) {
        return localizedContentRepository.findById(id);
    }

    @Override
    @NonNull
    public LocalizedContent persist(@NonNull Content content) {
        final LocalizedContent localizedContent = (LocalizedContent)content;
        return localizedContentRepository.save(localizedContent);
    }

    @Override
    @NonNull
    public MediaType getContentType() {
        return MediaType.TEXT;
    }
}
