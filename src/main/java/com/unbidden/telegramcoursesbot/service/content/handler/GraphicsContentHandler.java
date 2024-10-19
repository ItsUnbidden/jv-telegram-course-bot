package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentTextData;
import com.unbidden.telegramcoursesbot.model.content.GraphicsContent;
import com.unbidden.telegramcoursesbot.model.content.Photo;
import com.unbidden.telegramcoursesbot.model.content.Video;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.GraphicsContentRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import com.unbidden.telegramcoursesbot.repository.VideoRepository;
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
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class GraphicsContentHandler implements LocalizedContentHandler<GraphicsContent> {
    private final PhotoRepository photoRepository;

    private final VideoRepository videoRepository;

    private final GraphicsContentRepository graphicsContentRepository;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public GraphicsContent parseLocalized(@NonNull List<Message> messages, boolean isLocalized) {
        final List<Video> videos = new ArrayList<>();
        final List<Photo> photos = new ArrayList<>();
        String captions = null;

        for (Message message : messages) {
            if (message.hasVideo()) {
                final Video video = new Video(message.getVideo());
                video.setThumbnail(resolveThumbnail(message.getVideo().getThumbnail()));
                videos.add(video);
            }
            if (message.hasPhoto()) {
                final Photo photo = new Photo(message.getPhoto()
                        .get(message.getPhoto().size() - 1));
                photos.add(photo);
            }
            if (message.getCaption() != null && !message.getCaption().isEmpty()) {
                captions = message.getCaption();
            }
        }
        videoRepository.saveAll(videos);
        photoRepository.saveAll(photos);
        GraphicsContent graphicsContent = new GraphicsContent();
        if (captions != null) {
            graphicsContent.setData(new ContentTextData(captions, isLocalized));
        }
        graphicsContent.setPhotos(photos);
        graphicsContent.setVideos(videos);
        return graphicsContent;
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
        final GraphicsContent graphicsContent = (GraphicsContent)content;
        final SendMediaGroupBuilder builder = SendMediaGroup.builder()
                .chatId(user.getId())
                .protectContent(isProtected);

        Localization captionsLoc = null;
        if (graphicsContent.getData() != null) {
            if (graphicsContent.getData().isLocalization()) {
                captionsLoc = localizationLoader.getLocalizationForUser(
                        graphicsContent.getData().getData(), user);
            } else {
                captionsLoc = new Localization(graphicsContent.getData().getData());
            }
        }
        for (Video video : graphicsContent.getVideos()) {
            final InputMediaVideo inputMedia = new InputMediaVideo(video.getId());
            if (video.getThumbnail() != null) {
                inputMedia.setThumbnail(new InputFile(video.getThumbnail().getId()));
            }
            if (captionsLoc != null) {
                inputMedia.setCaption(captionsLoc.getData());
                inputMedia.setCaptionEntities(captionsLoc.getEntities());
            }
            inputMedias.add(inputMedia);
        }
        for (Photo photo : graphicsContent.getPhotos()) {
            final InputMediaPhoto inputMedia = new InputMediaPhoto(photo.getId());
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
            throw new TelegramException("Unable to send graphics media group in content "
                    + content.getId() + " to user " + user.getId(), e);
        }
    }

    @Override
    @NonNull
    public Optional<GraphicsContent> findById(@NonNull Long id) {
        return graphicsContentRepository.findById(id);
    }

    @Override
    @NonNull
    public GraphicsContent persist(@NonNull Content content) {
        final GraphicsContent graphicsContent = (GraphicsContent)content;
        return graphicsContentRepository.save(graphicsContent);
    }

    @Override
    @NonNull
    public MediaType getContentType() {
        return MediaType.GRAPHICS;
    }

    private Photo resolveThumbnail(PhotoSize extThumbnail) {
        if (extThumbnail != null) {
            final Optional<Photo> potentialThumbnail = photoRepository.findById(
                    extThumbnail.getFileUniqueId());
            if (potentialThumbnail.isPresent()) {
                return potentialThumbnail.get();
            } else {
                return photoRepository.save(new Photo(extThumbnail));
            }
        }
        return null;
    }
}
