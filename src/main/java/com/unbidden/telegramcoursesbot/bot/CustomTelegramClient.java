package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.GetWebhookInfo;
import org.telegram.telegrambots.meta.api.objects.WebhookInfo;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class CustomTelegramClient extends OkHttpTelegramClient {
    private static final Logger LOGGER = LogManager.getLogger(CustomTelegramClient.class);
    
    private static final String ERROR_SEND_MESSAGE_FAILURE = "error_send_message_failure";
    private static final String MENU_COMMAND_DESCRIPTION = "menu_command_%s_description";
    
    private static final List<String> COMMAND_MENU_EXCEPTIONS = new ArrayList<>();

    private volatile boolean isOnMaintenance;

    @Autowired
    private UserService userService;

    @Autowired
    private LocalizationLoader localizationLoader;

    public CustomTelegramClient(@Value("${telegram.bot.authorization.token}") String botToken) {
        super(botToken);
        COMMAND_MENU_EXCEPTIONS.add("/testcourse");
    }

    public WebhookInfo getInfo() {
        try {
            return execute(GetWebhookInfo.builder().build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to get webhook info.", null, e);
        }
    }

    public void setUpMenuButton() {
        SetChatMenuButton setChatMenuButton = SetChatMenuButton.builder()
                .menuButton(MenuButtonCommands.builder().build())
                .build();
        try {
            execute(setChatMenuButton);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set bot's menu button.", null, e);
        }
    }

    public void setUpUserMenu(@NonNull String languageCode,
            @NonNull List<String> userCommandNames) {
        final List<BotCommand> userCommands = parseToBotCommands(userCommandNames, languageCode);

        final SetMyCommands setMyUserCommands = SetMyCommands.builder()
                .commands(userCommands)
                .scope(BotCommandScopeDefault.builder().build())
                .languageCode(languageCode)
                .build();
        try {
            execute(setMyUserCommands);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set up a menu.", null, e);
        }
    }

    /**
     * Sends message provided in {@link SendMessage}. Warning! Field chatId in 
     * {@link SendMessage} must be a user id, if that is not the case, exception will be thrown.
     * @param sendMessage Telegram message builder
     * @return sent {@link Message}
     */
    public Message sendMessage(@NonNull SendMessage sendMessage) {
        final UserEntity user = userService.getUser(Long.parseLong(sendMessage.getChatId()));

        try {
            return execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send message.", localizationLoader
                    .getLocalizationForUser(ERROR_SEND_MESSAGE_FAILURE, user), e);
        }
    }

    /**
     * Sends message to {@link UserEntity} using provided {@link Localization}.
     * @param user to whom the message will be sent
     * @param localization
     * @return sent {@link Message}
     */
    @NonNull
    public Message sendMessage(@NonNull UserEntity user, @NonNull Localization localization) {
        try {
            return execute(SendMessage.builder()
                    .chatId(user.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send message.", localizationLoader
                    .getLocalizationForUser(ERROR_SEND_MESSAGE_FAILURE, user), e);
        }
    }

    /**
     * Sends message to {@link UserEntity} using provided {@link Localization}
     * with a specified markup.
     * @param user to whom the message will be sent
     * @param localization
     * @param replyMarkup 
     * @return sent {@link Message}
     */
    @NonNull
    public Message sendMessage(@NonNull UserEntity user, @NonNull Localization localization,
            @NonNull ReplyKeyboard replyMarkup) {
        try {
            return execute(SendMessage.builder()
                    .chatId(user.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .replyMarkup(replyMarkup)
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send message.", localizationLoader
                    .getLocalizationForUser(ERROR_SEND_MESSAGE_FAILURE, user), e);
        }
    }

    public boolean isOnMaintenance() {
        return isOnMaintenance;
    }

    public void setOnMaintenance(boolean isOnMaintenance) {
        this.isOnMaintenance = isOnMaintenance;
    }

    public void deleteAdminMenuForUser(@NonNull UserEntity user) {
        try {
            execute(DeleteMyCommands.builder()
                    .languageCode(user.getLanguageCode())
                    .scope(BotCommandScopeChat.builder()
                        .chatId(user.getId()).build())
                    .build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to clear commands for user " + user.getId()
                    + " and language code " + user.getLanguageCode(), null, e);
        }
        LOGGER.info("Admin menu for user " + user.getId() + " has been removed.");
    }

    public void setUpMenuForAdmin(@NonNull UserEntity user, @NonNull List<String> allCommands) {
        final List<BotCommand> adminCommands = parseToBotCommands(allCommands,
                user.getLanguageCode());

        try {
            execute(SetMyCommands.builder().commands(adminCommands)
                    .scope(BotCommandScopeChat.builder().chatId(user.getId()).build())
                    .languageCode(user.getLanguageCode()).build());
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to set admin commands for user " + user.getId()
                    + " and language code " + user.getLanguageCode(), null, e);
        }
        LOGGER.info("Admin menu for user " + user.getId() + " has been added.");
    }

    private List<BotCommand> parseToBotCommands(List<String> commands, String languageCode) {
        return commands.stream()
                .filter(c -> !COMMAND_MENU_EXCEPTIONS.contains(c))
                .map(c -> (BotCommand)BotCommand.builder()
                    .command(c)
                    .description(localizationLoader.loadLocalization(
                        MENU_COMMAND_DESCRIPTION.formatted(c.replace("/", "")),
                        languageCode).getData())
                    .build())
                .toList();
    }
}
