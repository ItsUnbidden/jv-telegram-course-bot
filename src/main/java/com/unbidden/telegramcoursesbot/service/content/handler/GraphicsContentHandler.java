package com.unbidden.telegramcoursesbot.service.content.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content;
import com.unbidden.telegramcoursesbot.model.content.ContentTextData;
import com.unbidden.telegramcoursesbot.model.content.GraphicsContent;
import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import com.unbidden.telegramcoursesbot.model.content.Photo;
import com.unbidden.telegramcoursesbot.model.content.Video;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.GraphicsContentRepository;
import com.unbidden.telegramcoursesbot.repository.MarkerAreaRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import com.unbidden.telegramcoursesbot.repository.VideoRepository;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
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
    private static final Logger LOGGER = LogManager.getLogger(GraphicsContentHandler.class);

    private final PhotoRepository photoRepository;

    private final VideoRepository videoRepository;

    private final GraphicsContentRepository graphicsContentRepository;

    private final MarkerAreaRepository markerAreaRepository;

    private final LocalizationLoader localizationLoader;
    
    private final TextContentHandler textContentHandler;

    private final UserService userService;

    private final TelegramBot bot;

    @Override
    public GraphicsContent parseLocalized(@NonNull List<Message> messages, boolean isLocalized) {
        return parse0(messages, null, isLocalized);
    }

    @Override
    public GraphicsContent parseLocalized(@NonNull List<Message> messages,
            @NonNull String localizationName, @NonNull String languageCode) {
        final GraphicsContent content = parse0(messages, localizationName, true);
        content.setLanguageCode(languageCode);
        return content;
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
        
        Localization captionsLoc = null;
        if (graphicsContent.getData() != null) {
            if (graphicsContent.getData().isLocalization()) {
                captionsLoc = localizationLoader.getLocalizationForUser(
                        graphicsContent.getData().getData(), user);
            } else {
                captionsLoc = new Localization(graphicsContent.getData().getData());
                captionsLoc.setEntities(graphicsContent.getData().getEntities().stream()
                        .map(m -> m.toMessageEntity()).toList());
            }
        }
        for (Video video : graphicsContent.getVideos()) {
            final InputMediaVideo inputMedia = new InputMediaVideo(video.getId());
            if (video.getThumbnail() != null) {
                inputMedia.setThumbnail(new InputFile(video.getThumbnail().getId()));
            }
            inputMedias.add(inputMedia);
        }
        for (Photo photo : graphicsContent.getPhotos()) {
            inputMedias.add(new InputMediaPhoto(photo.getId()));
        }

        if (inputMedias.isEmpty()) {
            LOGGER.warn("Content " + content.getId() + " is of type " + content.getType()
                    + " but does not have any relevant content. Text content handler "
                    + "will be used instead.");
            return textContentHandler.sendContent(content, user, isProtected);
        }

        if (captionsLoc != null) {
            inputMedias.get(0).setCaption(captionsLoc.getData());
            inputMedias.get(0).setCaptionEntities(captionsLoc.getEntities());
        }

        if (inputMedias.size() == 1) {
            final InputMedia inputMedia = inputMedias.get(0);
            LOGGER.debug("Graphics content " + content.getId() + " contains only one media.");

            if (inputMedia.getClass().equals(InputMediaPhoto.class)) {
                LOGGER.debug("The media is a photo.");
                try {
                    return List.of(bot.execute(SendPhoto.builder()
                            .chatId(user.getId())
                            .protectContent(isProtected)
                            .photo(new InputFile(inputMedia.getMedia()))
                            .caption((captionsLoc != null) ? captionsLoc.getData() : null)
                            .captionEntities((captionsLoc != null)
                                ? captionsLoc.getEntities() : List.of())
                            .build()));
                } catch (TelegramApiException e) {
                    throw new TelegramException("Unable to send photo media in content "
                            + content.getId() + " to user " + user.getId(), e);
                }
            }
            LOGGER.debug("The media is a video.");
            try {
                return List.of(bot.execute(SendVideo.builder()
                        .chatId(user.getId())
                        .protectContent(isProtected)
                        .video(new InputFile(inputMedia.getMedia()))
                        .caption((captionsLoc != null) ? captionsLoc.getData() : null)
                        .captionEntities((captionsLoc != null)
                            ? captionsLoc.getEntities() : List.of())
                        .build()));
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to send video media in content "
                        + content.getId() + " to user " + user.getId(), e);
            }
        }

        try {
            return bot.execute(SendMediaGroup.builder()
                    .chatId(user.getId())
                    .protectContent(isProtected)
                    .medias(inputMedias)
                    .build());
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

    private GraphicsContent parse0(List<Message> messages, String localizationName,
            boolean isLocalized) {
        final List<Video> videos = new ArrayList<>();
        final List<Photo> photos = new ArrayList<>();
        final List<MarkerArea> markers = new ArrayList<>();
        boolean isTextSetUp = localizationName != null;
        String languageCode = null;
        String captions = localizationName;

        for (Message message : messages) {
            if (!isTextSetUp && message.hasText()) {
                isTextSetUp = true;
                captions = message.getText();
                if (message.getEntities() != null
                        && !message.getEntities().isEmpty()) {
                    markers.addAll(message.getEntities().stream()
                        .map(e -> markerAreaRepository.save(new MarkerArea(e))).toList());
                }
                continue;
            }
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
            if (!isTextSetUp && message.getCaption() != null && !message.getCaption().isEmpty()) {
                captions = message.getCaption();
                if (message.getCaptionEntities() != null
                        && !message.getCaptionEntities().isEmpty()) {
                    markers.addAll(message.getCaptionEntities().stream()
                        .map(e -> markerAreaRepository.save(new MarkerArea(e))).toList());
                }
            }
            if (languageCode == null) {
                languageCode = userService.getUser(message.getFrom().getId()).getLanguageCode();
            }
        }
        videoRepository.saveAll(videos);
        photoRepository.saveAll(photos);
        GraphicsContent graphicsContent = new GraphicsContent();
        graphicsContent.setData(new ContentTextData(captions, markers, isLocalized));
        graphicsContent.setPhotos(photos);
        graphicsContent.setVideos(videos);
        graphicsContent.setLanguageCode(languageCode);
        return graphicsContent;
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
