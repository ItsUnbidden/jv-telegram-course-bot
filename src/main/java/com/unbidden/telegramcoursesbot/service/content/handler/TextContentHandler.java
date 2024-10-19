package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentTextData;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.LocalizedContentRepository;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class TextContentHandler implements LocalizedContentHandler<LocalizedContent> {
    private final LocalizedContentRepository localizedContentRepository;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public LocalizedContent parseLocalized(@NonNull List<Message> messages, boolean isLocalized) {
        final LocalizedContent localizedContent = new LocalizedContent();
        localizedContent.setData(new ContentTextData(messages.get(0).getText(), isLocalized));

        return localizedContent;
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user) {
        return sendContent(content, user, false);
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user,
            boolean isProtected) {
        final LocalizedContent localizedContent = (LocalizedContent)content;
        final Localization localization = (localizedContent.getData().isLocalization())
                ? localizationLoader.getLocalizationForUser(localizedContent
                    .getData().getData(), user)
                : new Localization(localizedContent.getData().getData());
        
        return List.of(bot.sendMessage(SendMessage.builder()
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
