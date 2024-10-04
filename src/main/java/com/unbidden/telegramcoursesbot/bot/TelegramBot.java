package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.Content;
import com.unbidden.telegramcoursesbot.model.Photo;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.Video;
import com.unbidden.telegramcoursesbot.repository.ContentRepository;
import com.unbidden.telegramcoursesbot.repository.PhotoRepository;
import com.unbidden.telegramcoursesbot.repository.VideoRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LogManager.getLogger(TelegramBot.class);

    private static final List<String> COMMAND_MENU_EXCEPTIONS = new ArrayList<>();

    @Value("${telegram.bot.authorization.username}")
    private String username;

    private volatile boolean isOnMaintenance;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired 
    private PhotoRepository photoRepository;

    @Autowired 
    private VideoRepository videoRepository;

    @Autowired
    @Lazy
    private CommandHandlerManager commandHandlerManager;

    @Autowired
    @Lazy
    private PaymentService paymentService;

    @Autowired
    @Lazy
    private MenuService menuService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserService userService;

    @Autowired
    private LocalizationLoader localizationLoader;

    public TelegramBot(@Autowired DefaultBotOptions botOptions,
            @Value("${telegram.bot.authorization.token}") String token) {
        super(botOptions, token);
        COMMAND_MENU_EXCEPTIONS.add("/testcourse");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().isCommand()) {
            userService.updateUser(update.getMessage().getFrom());
            final String[] commandParts = update.getMessage().getText().split(" ");

            LOGGER.info("Update with command " + update.getMessage().getText()
                    + " triggered by user " + update.getMessage().getFrom().getId() + ".");
            sessionService.removeSessionsForUser(update.getMessage().getFrom());
            commandHandlerManager.getHandler(commandParts[0]).handle(update.getMessage(),
                    commandParts);
            return;
        }
        if (update.hasPreCheckoutQuery()) {
            userService.updateUser(update.getPreCheckoutQuery().getFrom());
            sessionService.removeSessionsForUser(update.getPreCheckoutQuery().getFrom());
            paymentService.resolvePreCheckout(update.getPreCheckoutQuery());
            return;
        }
        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            userService.updateUser(update.getMessage().getFrom());
            sessionService.removeSessionsForUser(update.getMessage().getFrom());
            paymentService.resolveSuccessfulPayment(update.getMessage());
            return;
        }
        if (update.hasCallbackQuery()) {
            userService.updateUser(update.getCallbackQuery().getFrom());
            sessionService.removeSessionsForUser(update.getCallbackQuery().getFrom());
            menuService.processCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage()) {
            userService.updateUser(update.getMessage().getFrom());
            sessionService.processResponse(update.getMessage());
            return;
        }
    }

    public void setUpMenuButton() {
        SetChatMenuButton setChatMenuButton = SetChatMenuButton.builder()
                .menuButton(MenuButtonCommands.builder().build())
                .build();
        try {
            execute(setChatMenuButton);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set bot's menu button.", e);
        }
    }

    public void setUpMenus() {
        localizationLoader.getAvailableLanguageCodes().forEach(c -> setUpMenu(c));
        userService.getAdminList().forEach(a -> setUpMenusForAdmin(a.getId()));
    }

    public void setUpMenusForAdmin(@NonNull Long userId) {
        LOGGER.info("Setting up admin menu for user " + userId);
        localizationLoader.getAvailableLanguageCodes().forEach(c ->
                setUpMenuForAdmin(userId, c));
    }

    public void removeMenusForUser(@NonNull Long userId) {
        localizationLoader.getAvailableLanguageCodes().forEach(c ->
                deleteAdminMenuForUser(userId, c));
    }

    private void setUpMenu(String languageCode) {
        final List<BotCommand> userCommands = parseToBotCommands(commandHandlerManager
                .getUserCommands(), languageCode);

        SetMyCommands setMyUserCommands = SetMyCommands.builder()
                .commands(userCommands)
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(languageCode)
                .build();
        try {
            execute(setMyUserCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up a menu.", e);
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    public Message sendMessage(SendMessage sendMessage) {
        try {
            return execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send message.", e);
        }
    }

    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull User user) {
        return sendContent(content, userService.getUser(user.getId()));
    }
    
    @NonNull
    public List<Message> sendContent(@NonNull Content content, @NonNull UserEntity user) {
        InputMediaVideo inputVideo = null;
        List<InputMediaPhoto> inputPhotos = null;
        final Localization textLocalization = (content.getData() != null) ?
                localizationLoader.getLocalizationForUser(content.getData(), user) : null;
        
        LOGGER.info("Content " + content.getId() + " sending initiated...");
        if (content.getVideo() != null) {
            LOGGER.info("Content has a video.");
            inputVideo = new InputMediaVideo();
            if (textLocalization != null) {
                inputVideo.setCaption(textLocalization.getData());
                inputVideo.setCaptionEntities(textLocalization.getEntities());
            }
            inputVideo.setDuration(content.getVideo().getDuration());
            inputVideo.setHasSpoiler(false);
            inputVideo.setHeight(content.getVideo().getHeight());
            inputVideo.setMedia(content.getVideo().getId());
            inputVideo.setMediaName(content.getVideo().getName());
            inputVideo.setWidth(content.getVideo().getWidth());
        }

        if (content.getPhotos() != null && !content.getPhotos().isEmpty()) {
            LOGGER.info("Content has some photos.");
            inputPhotos = content.getPhotos().stream().map(p -> {
                    final InputMediaPhoto inputPhoto = new InputMediaPhoto();

                    inputPhoto.setMedia(p.getId());
                    if (textLocalization != null) {
                        inputPhoto.setCaption(textLocalization.getData());
                        inputPhoto.setCaptionEntities(textLocalization.getEntities());
                    }
                    return inputPhoto;
            }).toList();
        }
        
        try {
            if (inputPhotos != null) {
                if (inputPhotos.size() > 1 || inputVideo != null) {
                    LOGGER.info("There is a combination of photos or photos and a video.");
                    final List<InputMedia> medias = new ArrayList<>();

                    medias.addAll(inputPhotos);
                    if (inputVideo != null) {
                        medias.add(inputVideo);
                    }
                    LOGGER.info("Sending...");
                    return execute(SendMediaGroup.builder()
                            .chatId(user.getId())
                            .disableNotification(false)
                            .protectContent(true)
                            .medias(medias)
                            .build());
                }
                final InputMediaPhoto photo = inputPhotos.get(0);
                
                LOGGER.info("There is one photo. Sending...");
                return List.of(execute(SendPhoto.builder()
                        .photo(new InputFile(photo.getMedia()))
                        .caption(photo.getCaption())
                        .captionEntities(new ArrayList<>())
                        .chatId(user.getId())
                        .disableNotification(false)
                        .protectContent(true)
                        .hasSpoiler(false)
                        .build()));
            }
            if (inputVideo != null) {
                LOGGER.info("There is one video. Sending...");
                return List.of(execute(SendVideo.builder()
                        .video(new InputFile(inputVideo.getMedia()))
                        .caption(inputVideo.getCaption())
                        .captionEntities(new ArrayList<>())
                        .chatId(user.getId())
                        .disableNotification(false)
                        .duration(inputVideo.getDuration())
                        .hasSpoiler(false)
                        .height(inputVideo.getHeight())
                        .protectContent(true)
                        .width(inputVideo.getWidth())
                        .build()));
            }
            if (textLocalization != null) {
                LOGGER.info("There is only text. Sending...");
                return List.of(execute(SendMessage.builder()
                        .chatId(user.getId())
                        .disableNotification(false)
                        .entities(textLocalization.getEntities())
                        .protectContent(true)
                        .text(textLocalization.getData())
                        .build()));
            }
            LOGGER.warn("There is no relevant content present.");
            final Localization errorLocalization = localizationLoader.getLocalizationForUser(
                    "error_content_empty", user);
            return List.of(sendMessage(SendMessage.builder()
                    .chatId(user.getId())
                    .text(errorLocalization.getData())
                    .entities(errorLocalization.getEntities())
                    .build()));
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send content.", e);
        }
    }

    @NonNull
    public Content parseAndPersistContent(@NonNull Message message) {
        LOGGER.info("Initiating parsing of a message to content.");
        List<Photo> photos = null;
        Video video = null;
        String text = null;
        if (message.hasPhoto()) {
            LOGGER.info("Message has one or more photos.");
            final PhotoSize photoSize = message.getPhoto().get(message.getPhoto().size() - 1);

            photos = new ArrayList<>();
            
            final Photo photo = new Photo();
            photo.setId(photoSize.getFileId());
            photo.setUniqueId(photoSize.getFileUniqueId());
            photo.setSize(photoSize.getFileSize());
            photo.setHeight(photoSize.getHeight());
            photo.setWidth(photoSize.getWidth());       
            photoRepository.save(photo);  
            photos.add(photo); 
        }
        if (message.hasVideo()) {
            LOGGER.info("Message has a video.");
            video = new Video();
            video.setId(message.getVideo().getFileId());
            video.setUniqueId(message.getVideo().getFileUniqueId());
            video.setDuration(message.getVideo().getDuration());
            video.setHeight(message.getVideo().getHeight());
            video.setWidth(message.getVideo().getWidth());
            video.setMimeType(message.getVideo().getMimeType());
            video.setName(message.getVideo().getFileName());
            video.setSize(message.getVideo().getFileSize());
            videoRepository.save(video);
        }
        if (message.hasText()) {
            LOGGER.info("Message has text.");
            text = message.getText();
        }
        if (message.getCaption() != null && !message.getCaption().isBlank()) {
            LOGGER.info("Message has captions.");
            text = message.getCaption();
        }
        if ((photos == null || photos.isEmpty()) && video == null && text == null) {
            throw new InvalidDataSentException("User " + message.getFrom().getId()
                    + " has sent invalid homework response.");
        }
        final Content content = new Content();
        content.setData(text);
        content.setVideo(video);
        content.setPhotos(photos);
        LOGGER.info("Persisting content...");
        return contentRepository.save(content);
    }

    public boolean isOnMaintenance() {
        return isOnMaintenance;
    }

    public void setOnMaintenance(boolean isOnMaintenance) {
        this.isOnMaintenance = isOnMaintenance;
    }

    private void deleteAdminMenuForUser(@NonNull Long userId, @NonNull String languageCode) {
        try {
            execute(DeleteMyCommands.builder()
                    .languageCode(languageCode)
                    .scope(BotCommandScopeChat.builder()
                        .chatId(userId).build())
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to clear commands for user " + userId
                    + " and language code " + languageCode, e);
        }
    }

    private void setUpMenuForAdmin(@NonNull Long userId, @NonNull String languageCode) {
        final List<BotCommand> userCommands = parseToBotCommands(commandHandlerManager
                .getUserCommands(), languageCode);
        final List<BotCommand> adminCommands = new ArrayList<>(parseToBotCommands(
                commandHandlerManager.getAdminCommands(), languageCode));
        adminCommands.addAll(userCommands);

        try {
            execute(SetMyCommands.builder().commands(adminCommands)
                    .scope(BotCommandScopeChat.builder().chatId(userId).build())
                    .languageCode(languageCode).build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set admin commands for user " + userId
                    + " and language code " + languageCode, e);
        }
    }

    private List<BotCommand> parseToBotCommands(List<String> commands, String languageCode) {
        return commands.stream()
                .filter(c -> !COMMAND_MENU_EXCEPTIONS.contains(c))
                .map(c -> BotCommand.builder()
                    .command(c)
                    .description(localizationLoader.loadLocalization(
                        "menu_command_" + c.replace("/", "") + "_description",
                        languageCode).getData())
                    .build())
                .toList();
    }
}
