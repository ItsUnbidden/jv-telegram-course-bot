package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Audio;
import com.unbidden.telegramcoursesbot.model.content.AudioContent;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import com.unbidden.telegramcoursesbot.model.content.Photo;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.AudioContentRepository;
import com.unbidden.telegramcoursesbot.repository.AudioRepository;
import com.unbidden.telegramcoursesbot.repository.MarkerAreaRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class AudioContentHandler implements LocalizedContentHandler<AudioContent> {
    private static final Logger LOGGER = LogManager.getLogger(AudioContentHandler.class);

    private final PhotoRepository photoRepository;

    private final AudioRepository audioRepository;

    private final AudioContentRepository audioContentRepository;

    private final MarkerAreaRepository markerAreaRepository;

    private final TextContentHandler textContentHandler;

    private final ClientManager clientManager;

    @Override
    @Transactional
    public AudioContent parseAndPersist(@NonNull Bot bot, @NonNull List<Message> messages,
            @NonNull String languageCode, boolean isProtected) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        
        final AudioContent audioContent = new AudioContent();
        final List<Audio> audios = new ArrayList<>();
        final List<MarkerArea> markers = new ArrayList<>();

        String captions = null;
        for (final Message message : messages) {
            if (captions == null && message.hasText()) {
                captions = message.getText();
                if (message.getEntities() != null) {
                    markers.addAll(message.getEntities().stream()
                            .map(e -> new MarkerArea(e, audioContent)).toList());
                }
                continue;
            }
            if (message.hasAudio()) {
                final Audio audio = new Audio(message.getAudio());

                if (message.getAudio().getThumbnail() != null) {
                    final Optional<Photo> potentialThumbnail = photoRepository.findById(
                            message.getAudio().getThumbnail().getFileUniqueId());

                    if (potentialThumbnail.isPresent()) {
                        audio.setThumbnail(potentialThumbnail.get());
                    } else {
                        audio.setThumbnail(photoRepository.save(
                                new Photo(message.getAudio().getThumbnail())));
                    }
                }
                audios.add(audio);
            }
            if (captions == null && message.getCaption() != null && !message.getCaption().isBlank()) {
                captions = message.getCaption();
                if (message.getCaptionEntities() != null) {
                    markers.addAll(message.getCaptionEntities().stream()
                        .map(e -> new MarkerArea(e, audioContent)).toList());
                }
            }
        }
        audioRepository.saveAll(audios);
        audioContent.setBot(bot);
        audioContent.setData(captions);
        audioContent.setAudios(audios);
        audioContent.setLanguageCode(languageCode);
        audioContent.setType(getContentType());
        audioContent.setProtected(isProtected);
        audioContentRepository.save(audioContent);
        markerAreaRepository.saveAll(markers);

        return audioContent;
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull UserEntity user, @NonNull Bot bot, @NonNull LocalizedContent content) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");

        final List<InputMedia> inputMedias = new ArrayList<>();
        final AudioContent audioContent = (AudioContent)content;
        audioContent.setAudios(audioRepository.findByContent(audioContent.getId()));

        for (final Audio audio : audioContent.getAudios()) {
            final InputMediaAudio inputMedia = new InputMediaAudio(audio.getId());

            if (audio.getThumbnail() != null) {
                inputMedia.setThumbnail(new InputFile(audio.getThumbnail().getId()));
            }
            inputMedias.add(inputMedia);
        }

        if (inputMedias.isEmpty()) {
            LOGGER.warn("Content " + content.getId() + " is of type " + content.getType()
                    + " but does not have any relevant content. Text content handler "
                    + "will be used instead.");
            return textContentHandler.sendContent(user, bot, content);
        }

        if (content.getData() != null) {
            inputMedias.get(inputMedias.size() - 1).setCaption(content.getData());
            inputMedias.get(inputMedias.size() - 1).setCaptionEntities(markerAreaRepository.findByContentId(
                    content.getId()).stream().map(MarkerArea::toMessageEntity).toList());
        }

        if (inputMedias.size() == 1) {
            final InputMedia inputMedia = inputMedias.get(0);
            LOGGER.debug("Audio content " + content.getId() + " contains only one media.");

            try {
                return List.of(clientManager.getClient(bot).execute(SendAudio.builder()
                        .chatId(user.getId())
                        .protectContent(content.isProtected())
                        .audio(new InputFile(inputMedia.getMedia()))
                        .caption(inputMedia.getCaption())
                        .captionEntities(inputMedia.getCaptionEntities())
                        .build()));
            } catch (TelegramApiException e) {
                LOGGER.error("Unable to send audio media in content " + content.getId()
                        + " to user " + user.getId(), e);
                return List.of();
            }
        }

        try {
            return clientManager.getClient(bot).execute(SendMediaGroup.builder()
                    .chatId(user.getId())
                    .protectContent(content.isProtected())
                    .medias(inputMedias)
                    .build());
        } catch (TelegramApiException e) {
            LOGGER.error("Unable to send audios media group in content " + content.getId()
                    + " to user " + user.getId(), e);
            return List.of();
        }
    }

    @Override
    @NonNull
    public MediaType getContentType() {
        return MediaType.AUDIO;
    }
}
