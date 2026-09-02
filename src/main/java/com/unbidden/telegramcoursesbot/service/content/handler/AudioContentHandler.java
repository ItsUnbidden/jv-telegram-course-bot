package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
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
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public class AudioContentHandler extends AbstractContentHandler<AudioContent> {
    private static final Logger LOGGER = LogManager.getLogger(AudioContentHandler.class);

    private final PhotoRepository photoRepository;

    private final AudioRepository audioRepository;

    private final AudioContentRepository audioContentRepository;

    private final MarkerAreaRepository markerAreaRepository;

    private final TextContentHandler textContentHandler;

    private final ClientManager clientManager;

    public AudioContentHandler(LocalizationLoader localizationLoader, PhotoRepository photoRepository,
            AudioRepository audioRepository, AudioContentRepository audioContentRepository,
            MarkerAreaRepository markerAreaRepository, TextContentHandler textContentHandler,
            ClientManager clientManager) {
        super(localizationLoader);
        this.photoRepository = photoRepository;
        this.audioRepository = audioRepository;
        this.audioContentRepository = audioContentRepository;
        this.markerAreaRepository = markerAreaRepository;
        this.textContentHandler = textContentHandler;
        this.clientManager = clientManager;
    }

    @Override
    @Transactional
    public AudioContent parseAndPersist(BotRole botRole, List<Message> messages, String languageCode, boolean isProtected) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");
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
        audioContent.setBot(botRole.getBot());
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
    public List<CompletableFuture<List<SendMessageResultDto>>> sendContentInBulkAsync(List<BotRole> targetRoles, LocalizedContent content) {
        Assert.notEmpty(targetRoles, "targetRoles cannot be empty or null");
        Assert.noNullElements(targetRoles, "targetRoles cannot contain null");
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
            return textContentHandler.sendContentInBulkAsync(targetRoles, content);
        }

        if (content.getData() != null) {
            inputMedias.get(inputMedias.size() - 1).setCaption(content.getData());
            inputMedias.get(inputMedias.size() - 1).setCaptionEntities(markerAreaRepository.findByContentId(
                    content.getId()).stream().map(MarkerArea::toMessageEntity).toList());
        }

        if (inputMedias.size() == 1) {
            final InputMedia inputMedia = inputMedias.get(0);
            final InputFile inputFile = new InputFile(inputMedia.getMedia());

            LOGGER.debug("Audio content " + content.getId() + " contains only one media.");
            return targetRoles.stream().map(br -> clientManager.getClient(br.getBot()).executeAsync(SendAudio.builder()
                    .chatId(br.getUser().getId())
                    .protectContent(content.isProtected())
                    .audio(inputFile)
                    .caption(inputMedia.getCaption())
                    .captionEntities(inputMedia.getCaptionEntities())
                    .build()).handle((m, t) -> {
                        if (t != null) {
                            return List.of(new SendMessageResultDto(new TelegramException("Failed to send an audio.",
                                    localizationLoader.localize(Localizations.Error.SEND_CONTENT, br), t)));
                        } else {
                            return List.of(new SendMessageResultDto(m));
                        }
                    })).toList();
        }

        return targetRoles.stream().map(br -> clientManager.getClient(br.getBot()).executeAsync(SendMediaGroup.builder()
                .chatId(br.getUser().getId())
                .protectContent(content.isProtected())
                .medias(inputMedias)
                .build()).handle((l, t) -> {
                    if (t != null) {
                        return List.of(new SendMessageResultDto(new TelegramException("Failed to send an audio media group.",
                                localizationLoader.localize(Localizations.Error.SEND_CONTENT, br), t)));
                    } else {
                        return l.stream().map(m -> new SendMessageResultDto(m)).toList();
                    }
                })).toList();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.AUDIO;
    }
}
