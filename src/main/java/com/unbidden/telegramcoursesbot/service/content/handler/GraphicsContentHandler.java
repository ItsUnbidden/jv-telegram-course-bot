package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
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
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

@Component
public class GraphicsContentHandler extends AbstractContentHandler<GraphicsContent> {
    private static final Logger LOGGER = LogManager.getLogger(GraphicsContentHandler.class);

    private final PhotoRepository photoRepository;

    private final VideoRepository videoRepository;

    private final GraphicsContentRepository graphicsContentRepository;

    private final MarkerAreaRepository markerAreaRepository;
    
    private final TextContentHandler textContentHandler;

    private final ClientManager clientManager;

    public GraphicsContentHandler(LocalizationLoader localizationLoader, PhotoRepository photoRepository,
            VideoRepository videoRepository, GraphicsContentRepository graphicsContentRepository,
            MarkerAreaRepository markerAreaRepository, TextContentHandler textContentHandler,
            ClientManager clientManager) {
        super(localizationLoader);
        this.photoRepository = photoRepository;
        this.videoRepository = videoRepository;
        this.graphicsContentRepository = graphicsContentRepository;
        this.markerAreaRepository = markerAreaRepository;
        this.textContentHandler = textContentHandler;
        this.clientManager = clientManager;
    }

    @Override
    @Transactional
    public GraphicsContent parseAndPersist(BotRole botRole, List<Message> messages, String languageCode, boolean isProtected) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");
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
        
        graphicsContent.setBot(botRole.getBot());
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
    public List<CompletableFuture<List<SendMessageResultDto>>> sendContentInBulkAsync(List<BotRole> targetRoles, LocalizedContent content) {
        Assert.notEmpty(targetRoles, "targetRoles cannot be empty or null");
        Assert.noNullElements(targetRoles, "targetRoles cannot contain null");
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
            
            LOGGER.debug("Graphics content " + content.getId() + " contains only one media.");
            if (inputMedia.getClass().equals(InputMediaPhoto.class)) {
                LOGGER.debug("The media is a photo.");

                return targetRoles.stream().map(br -> clientManager.getClient(br.getBot()).executeAsync(SendPhoto.builder()
                        .chatId(br.getUser().getId())
                        .protectContent(content.isProtected())
                        .photo(inputFile)
                        .caption(inputMedia.getCaption())
                        .captionEntities(inputMedia.getCaptionEntities())
                        .build()).handle((m, t) -> {
                            if (t != null) {
                                return List.of(new SendMessageResultDto(new TelegramException("Failed to send photo.",
                                        localizationLoader.localize(Localizations.Error.SEND_CONTENT, br), t)));
                            } else {
                                return List.of(new SendMessageResultDto(m));
                            }
                        })).toList();
            }
            LOGGER.debug("The media is a video.");
            return targetRoles.stream().map(br -> clientManager.getClient(br.getBot()).executeAsync(SendVideo.builder()
                    .chatId(br.getUser().getId())
                    .protectContent(content.isProtected())
                    .video(inputFile)
                    .caption(inputMedia.getCaption())
                    .captionEntities(inputMedia.getCaptionEntities())
                    .build()).handle((m, t) -> {
                        if (t != null) {
                            return List.of(new SendMessageResultDto(new TelegramException("Failed to send a video.",
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
                        return List.of(new SendMessageResultDto(new TelegramException("Failed to send graphics media group.",
                                localizationLoader.localize(Localizations.Error.SEND_CONTENT, br), t)));
                    } else {
                        return l.stream().map(m -> new SendMessageResultDto(m)).toList();
                    }
                })).toList();
    }

    @Override
    public MediaType getContentType() {
        return MediaType.GRAPHICS;
    }
}
