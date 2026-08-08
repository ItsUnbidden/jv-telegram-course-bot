package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.GraphicsContent;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import com.unbidden.telegramcoursesbot.model.content.Photo;
import com.unbidden.telegramcoursesbot.model.content.Video;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.GraphicsContentRepository;
import com.unbidden.telegramcoursesbot.repository.MarkerAreaRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import com.unbidden.telegramcoursesbot.repository.VideoRepository;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class GraphicsContentHandler implements LocalizedContentHandler<GraphicsContent> {
    private static final Logger LOGGER = LogManager.getLogger(GraphicsContentHandler.class);

    private final PhotoRepository photoRepository;

    private final VideoRepository videoRepository;

    private final GraphicsContentRepository graphicsContentRepository;

    private final MarkerAreaRepository markerAreaRepository;

    
    private final TextContentHandler textContentHandler;

    private final ClientManager clientManager;

    @NonNull
    @Override
    @Transactional
    public GraphicsContent parseAndPersist(@NonNull Bot bot, @NonNull List<Message> messages,
            @NonNull String languageCode, boolean isProtected) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        
        final GraphicsContent graphicsContent = new GraphicsContent();
        final List<Video> videos = new ArrayList<>();
        final List<Photo> photos = new ArrayList<>();
        final List<MarkerArea> markers = new ArrayList<>();

        String captions = null;
        for (final Message message : messages) {
            if (captions == null && message.hasText()) {
                captions = message.getText();
                if (message.getEntities() != null) {
                    markers.addAll(message.getEntities().stream()
                        .map(e -> markerAreaRepository.save(new MarkerArea(e, graphicsContent))).toList());
                }
                continue;
            }
            if (message.hasVideo()) {
                final Video video = new Video(message.getVideo());

                if (message.getVideo().getThumbnail() != null) {
                    final Optional<Photo> potentialThumbnail = photoRepository.findById(
                            message.getVideo().getThumbnail().getFileUniqueId());

                    if (potentialThumbnail.isPresent()) {
                        video.setThumbnail(potentialThumbnail.get());
                    } else {
                        video.setThumbnail(photoRepository.save(
                                new Photo(message.getVideo().getThumbnail())));
                    }
                }
                videos.add(video);
            }
            if (message.hasPhoto()) {
                photos.add(new Photo(message.getPhoto().get(message.getPhoto().size() - 1)));
            }
            if (captions == null && message.getCaption() != null && !message.getCaption().isEmpty()) {
                captions = message.getCaption();
                if (message.getCaptionEntities() != null) {
                    markers.addAll(message.getCaptionEntities().stream()
                            .map(e -> new MarkerArea(e, graphicsContent)).toList());
                }
            }
        }
        videoRepository.saveAll(videos);
        photoRepository.saveAll(photos);
        
        graphicsContent.setBot(bot);
        graphicsContent.setData(captions);
        graphicsContent.setPhotos(photos);
        graphicsContent.setVideos(videos);
        graphicsContent.setLanguageCode(languageCode);
        graphicsContent.setType(getContentType());
        graphicsContent.setProtected(isProtected);
        graphicsContentRepository.save(graphicsContent);
        markerAreaRepository.saveAll(markers);

        return graphicsContent;
    }

    @Override
    @NonNull
    public List<Message> sendContent(@NonNull UserEntity user, @NonNull Bot bot, @NonNull LocalizedContent content) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(content, "content cannot be null");

        final List<InputMedia> inputMedias = new ArrayList<>();
        final GraphicsContent graphicsContent = (GraphicsContent)content;
        graphicsContent.setPhotos(photoRepository.findByContent(graphicsContent.getId()));
        graphicsContent.setVideos(videoRepository.findByContent(graphicsContent.getId()));
        
        for (final Video video : graphicsContent.getVideos()) {
            final InputMediaVideo inputMedia = new InputMediaVideo(video.getId());

            if (video.getThumbnail() != null) {
                inputMedia.setThumbnail(new InputFile(video.getThumbnail().getId()));
            }
            inputMedias.add(inputMedia);
        }
        graphicsContent.getPhotos().forEach(p -> inputMedias.add(new InputMediaPhoto(p.getId())));

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
            LOGGER.debug("Graphics content " + content.getId() + " contains only one media.");

            if (inputMedia.getClass().equals(InputMediaPhoto.class)) {
                LOGGER.debug("The media is a photo.");
                try {
                    return List.of(clientManager.getClient(bot).execute(SendPhoto.builder()
                            .chatId(user.getId())
                            .protectContent(content.isProtected())
                            .photo(new InputFile(inputMedia.getMedia()))
                            .caption(inputMedia.getCaption())
                            .captionEntities(inputMedia.getCaptionEntities())
                            .build()));
                } catch (TelegramApiException e) {
                    LOGGER.error("Unable to send photo media in content " + content.getId()
                            + " to user " + user.getId(), e);
                    return List.of();
                }
            }
            LOGGER.debug("The media is a video.");
            try {
                return List.of(clientManager.getClient(bot).execute(SendVideo.builder()
                        .chatId(user.getId())
                        .protectContent(content.isProtected())
                        .video(new InputFile(inputMedia.getMedia()))
                        .caption(inputMedia.getCaption())
                        .captionEntities(inputMedia.getCaptionEntities())
                        .build()));
            } catch (TelegramApiException e) {
                LOGGER.error("Unable to send video media in content " + content.getId()
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
            LOGGER.error("Unable to send graphics media group in content " + content.getId()
                    + " to user " + user.getId(), e);
            return List.of();
        }
    }

    @Override
    @NonNull
    public MediaType getContentType() {
        return MediaType.GRAPHICS;
    }
}
