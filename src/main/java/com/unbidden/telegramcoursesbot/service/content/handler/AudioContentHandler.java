package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Audio;
import com.unbidden.telegramcoursesbot.model.content.AudioContent;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentTextData;
import com.unbidden.telegramcoursesbot.model.content.Photo;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.AudioContentRepository;
import com.unbidden.telegramcoursesbot.repository.AudioRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup.SendMediaGroupBuilder;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class AudioContentHandler implements LocalizedContentHandler<AudioContent> {
    private final PhotoRepository photoRepository;

    private final AudioRepository audioRepository;

    private final AudioContentRepository audioContentRepository;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public AudioContent parseLocalized(@NonNull List<Message> messages, boolean isLocalized) {
        final List<Audio> audios = new ArrayList<>();
        String captions = null;

        for (Message message : messages) {
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
            if (message.getCaption() != null && !message.getCaption().isEmpty()) {
                captions = message.getCaption();
            }
            audios.add(audio);
        }
        audioRepository.saveAll(audios);
        AudioContent audioContent = new AudioContent();
        if (captions != null) {
            audioContent.setData(new ContentTextData(captions, isLocalized));
        }
        audioContent.setAudios(audios);
        return audioContent;
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
        final List<InputMedia> inputMedias = new ArrayList<>();
        final AudioContent audioContent = (AudioContent)content;
        final SendMediaGroupBuilder builder = SendMediaGroup.builder()
                .chatId(user.getId())
                .protectContent(isProtected);

        Localization captionsLoc = null;
        if (audioContent.getData() != null) {
            if (audioContent.getData().isLocalization()) {
                captionsLoc = localizationLoader.getLocalizationForUser(
                        audioContent.getData().getData(), user);
            } else {
                captionsLoc = new Localization(audioContent.getData().getData());
            }
        }
        for (Audio audio : audioContent.getAudios()) {
            final InputMediaAudio inputMedia = new InputMediaAudio(audio.getId());
            if (audio.getThumbnail() != null) {
                inputMedia.setThumbnail(new InputFile(audio.getThumbnail().getId()));
            }
            if (captionsLoc != null) {
                inputMedia.setCaption(captionsLoc.getData());
                inputMedia.setCaptionEntities(captionsLoc.getEntities());
            }
            inputMedias.add(inputMedia);
        }
        builder.medias(inputMedias);
        try {
            return bot.execute(builder.build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send audios media group in content "
                    + content.getId() + " to user " + user.getId(), e);
        }
    }

    @Override
    @NonNull
    public Optional<AudioContent> findById(@NonNull Long id) {
        return audioContentRepository.findById(id);
    }

    @Override
    @NonNull
    public AudioContent persist(@NonNull Content content) {
        final AudioContent audioContent = (AudioContent)content;
        return audioContentRepository.save(audioContent);
    }

    @Override
    @NonNull
    public MediaType getContentType() {
        return MediaType.AUDIO;
    }
}
